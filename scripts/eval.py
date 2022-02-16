import argparse
import os
import queue
import re
import signal
import subprocess
import sys
import threading
import time
from datetime import datetime

import numpy as np
import pandas as pd
import requests

# Keep track of subprocesses and threads, and a queue for writing in order
procs = []
threads = []
writequeue = queue.Queue()

# Used to signal that it has been detected that the data manager preloading has finished, meaning that we can start firing experiments
preloading_finished = False

# Used to stop the thread when an experiment is done and the data manager needs to be restarted
break_dm_thread = False

# Used to track whether an error occurred, e.g. a non-200 status code
error_occurred = False

# Keep track of some additional information captured from the logs
matching_times = []
get_policies_times = []

if not os.path.exists('tmp'):
    os.makedirs('tmp')
logfile = open(f'tmp/log_{datetime.today().strftime("%Y-%m-%d_%H-%M-%S")}.txt', 'w')

def log(str):
    """Log str immediately to stdout"""
    sys.stdout.write(str)
    sys.stdout.flush()
    logfile.write(str)


def enqueue_output(out, id, queue):
    """
    Enqueue some output in order to be written in order later on.
    Neatly prints the id in front of the line.
    out: output (i.e. of stdout of a process)
    id: id for the process
    queue: the queue to enqueue to
    """
    for line in iter(out.readline, b''):
        if break_dm_thread:
            out.close()
            break

        prefix = str(id)
        if len(prefix) <= 4:
            prefix = f"{id}{' ' * (4 - len(prefix))}"
        
        check_line(id, line)
        queue.put(f"{prefix}| {line}")
    out.close()


def check_line(id, line):
    if id == "DM" and "Finished preloading" in line:
        global preloading_finished
        preloading_finished = True
    if id == "CC" and "policy matching and conflict resolution" in line:
        matching_time = int(re.search(r'(\d*)  \d\d\d%  policy matching and conflict resolution', line).group(1))
        matching_times.append(matching_time)
    if id == "CC" and "get policies from db" in line:
        get_policies_time = int(re.search(r'(\d*)  \d\d\d%  get policies from db', line).group(1))
        get_policies_times.append(get_policies_time)

def start_compliance_checker():
    log("----- Starting compliance checker -----\n")
    cc_cmd = [ 'java', '-jar', 'compliance-checker/build/libs/compliance-checker-0.1-SNAPSHOT.jar']
    proc = subprocess.Popen(cc_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
    procs.append(proc)

    thread = threading.Thread(target=enqueue_output,
                              args=(proc.stdout, "CC", writequeue))
    thread.daemon = True
    thread.start()
    threads.append(thread)


def start_data_manager(args):
    log("----- Starting data manager -----\n")
    dm_cmd = [ 'java' ] + args + [ '-jar', 'data-manager/build/libs/data-manager-0.1-SNAPSHOT.jar' ]
    proc = subprocess.Popen(dm_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
    procs.append(proc)

    thread = threading.Thread(target=enqueue_output,
                              args=(proc.stdout, "DM", writequeue))
    thread.daemon = True
    thread.start()
    threads.append(thread)


def print_output():
    """Continuously check the queue for any new output and log it"""
    while True:
        try:
            line = writequeue.get_nowait()
        except queue.Empty:
            time.sleep(.05)
            pass
        else:
            log(line)

        if all(proc.poll() is not None for proc in procs):
            return


def stop(sig, frame):
    """Gracefully stop all the processes"""
    log("\nStopping...\n")
    if error_occurred:
        log("Note: An error occurred!")
    for proc in procs:
        proc.terminate()
    logfile.close()
    sys.exit(0)



######## Main #########
def parse_args():
    """Parse the arguments of the script"""
    parser = argparse.ArgumentParser(description="Run the evaluation script")

    parser.add_argument('-s', '--skip-build', action='store_true',
                        help="skip the Gradle build of the project")

    return parser.parse_args()


signal.signal(signal.SIGINT, stop)
args = parse_args()

if not args.skip_build:
    subprocess.run("./gradlew build", shell=True, check=True)

# Start a thread for monitoring the other threads
print_thread = threading.Thread(target=print_output)
print_thread.daemon = True
print_thread.start()

# The compliance checker can just keep running, we only have to switch out the data
start_compliance_checker()



######## Experiments #########

def send_request(run):
    r = requests.post("http://localhost:8081/validation", json={
        "sql": f"SELECT * FROM \"TPCW{run}\".\"ITEM\"",
        "attributes": {
            "purpose": ["Product Improvement"],
            "role": ["Engineer"]
        }
    }, headers={
        'Content-type':'application/json',
        'Accept':'application/json'
    })
    if r.status_code != 200:
        global error_occurred
        error_occurred = True
        log(f"ERROR: {r.status_code}: {r.text}\n")
    return r.status_code


def run_single_experiment(name, args):
    log(f"----- Starting experiment {name} -----\n")
    global break_dm_thread
    break_dm_thread = False
    start_data_manager(args)

    global preloading_finished
    while not preloading_finished:
        time.sleep(1)
    
    if preloading_finished:
        preloading_finished = False

        # Warmup request
        send_request(0)

        # Store 5 runs per experiment
        times = np.zeros(5)

        for i in range(5):
            start = time.perf_counter()
            r = send_request(i)
            end = time.perf_counter()
            
            if r == 200:
                times[i] = end - start
        
        log("Done with experiment, terminating this data manager...\n")
        procs[1].terminate()
        procs.pop()
        break_dm_thread = True
        threads[1].join()
        threads.pop()

        return np.mean(times)


def process_log_times(arr, outer_vars, inner_vars):
    arr = np.asarray(arr)
    # Drop every 6th item because it is a warmup result
    arr = np.delete(arr, np.arange(0, arr.size, 6))
    # Calculate the average over every 5 runs
    arr = np.mean(arr.reshape(-1, 5), axis=1)
    # Convert to seconds
    arr = np.asarray(np.array_split(arr, len(outer_vars))) / 1000000000
    # Return a dataframe in the same shape as the 'regular' experiment result
    return pd.DataFrame(np.transpose(arr), index=inner_vars, columns=outer_vars)
        

def run_processing_time_experiment():
    N_POLICIES_PER_TAG = [100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]
    N_SCHEMAS = [10, 20, 30, 40, 50]
    N_CONTEXT = [10, 20, 30, 40, 50]
    N_REQUIREMENTS = [10, 20, 30, 40, 50]

    fixed_experiment_args = [
        '-Dspring.jpa.hibernate.ddl-auto=create',
        '-Ddatamanager.usecasedata=false',
        '-Ddatamanager.generatedata=false',
        '-Ddatamanager.tpcwdata=true',
    ]

    default_experiment_variables = {
        '-Ddatamanager.generatedata.npoliciespertag=': 100,
        '-Ddatamanager.generatedata.nschemas=': 5,
        '-Ddatamanager.generatedata.ncontextattrs=': 10,
        '-Ddatamanager.generatedata.nrequirementattrs=': 10,
    }


    #### Number of policies vs Number of context attributes ####
    res = np.zeros([len(N_CONTEXT), len(N_POLICIES_PER_TAG)])

    for npolicies in N_POLICIES_PER_TAG:
        current_experiment_variables = default_experiment_variables.copy()
        current_experiment_variables['-Ddatamanager.generatedata.npoliciespertag='] = npolicies

        for nattributes in N_CONTEXT:
            current_experiment_variables['-Ddatamanager.generatedata.ncontextattrs='] = nattributes
            current_experiment_args = fixed_experiment_args + [key + str(value) for key, value in current_experiment_variables.items()]
            name = f"exp-npol{npolicies}-ncon{nattributes}"
            avg_time = run_single_experiment(name, current_experiment_args)
            res[N_CONTEXT.index(nattributes), N_POLICIES_PER_TAG.index(npolicies)] = avg_time

    df = pd.DataFrame(res, index=N_CONTEXT, columns=N_POLICIES_PER_TAG)
    df.to_csv('tmp/exp-npol-ncon.csv')

    df = process_log_times(matching_times, N_POLICIES_PER_TAG, N_CONTEXT)
    df.to_csv('tmp/exp-npol-ncon-matching.csv')
    matching_times.clear()

    df = process_log_times(get_policies_times, N_POLICIES_PER_TAG, N_CONTEXT)
    df.to_csv('tmp/exp-npol-ncon-get-policies.csv')
    get_policies_times.clear()


    #### Number of policies vs Number of requirement attributes ####
    res = np.zeros([len(N_REQUIREMENTS), len(N_POLICIES_PER_TAG)])

    for npolicies in N_POLICIES_PER_TAG:
        current_experiment_variables = default_experiment_variables.copy()
        current_experiment_variables['-Ddatamanager.generatedata.npoliciespertag='] = npolicies

        for nattributes in N_REQUIREMENTS:
            current_experiment_variables['-Ddatamanager.generatedata.nrequirementattrs='] = nattributes
            current_experiment_args = fixed_experiment_args + [key + str(value) for key, value in current_experiment_variables.items()]
            name = f"exp-npol{npolicies}-nreq{nattributes}"
            avg_time = run_single_experiment(name, current_experiment_args)
            res[N_REQUIREMENTS.index(nattributes), N_POLICIES_PER_TAG.index(npolicies)] = avg_time

    df = pd.DataFrame(res, index=N_REQUIREMENTS, columns=N_POLICIES_PER_TAG)
    df.to_csv('tmp/exp-npol-nreq.csv')

    df = process_log_times(matching_times, N_POLICIES_PER_TAG, N_REQUIREMENTS)
    df.to_csv('tmp/exp-npol-nreq-matching.csv')
    matching_times.clear()

    df = process_log_times(get_policies_times, N_POLICIES_PER_TAG, N_REQUIREMENTS)
    df.to_csv('tmp/exp-npol-nreq-get-policies.csv')
    get_policies_times.clear()


    #### Large number of policies without changing other parameters ####
    N_POLICIES_PER_TAG = [500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000]
    res = np.zeros(len(N_POLICIES_PER_TAG))

    for npolicies in N_POLICIES_PER_TAG:
        current_experiment_variables = default_experiment_variables.copy()
        current_experiment_variables['-Ddatamanager.generatedata.npoliciespertag='] = npolicies
        current_experiment_args = fixed_experiment_args + [key + str(value) for key, value in current_experiment_variables.items()]
        name = f"exp-npol{npolicies}"
        avg_time = run_single_experiment(name, current_experiment_args)
        res[N_POLICIES_PER_TAG.index(npolicies)] = avg_time

    df = pd.DataFrame([res], columns=N_POLICIES_PER_TAG)
    df.to_csv('tmp/exp-npol.csv')

    df = process_log_times(matching_times, N_POLICIES_PER_TAG, None)
    df.to_csv('tmp/exp-npol-matching.csv')
    matching_times.clear()

    df = process_log_times(get_policies_times, N_POLICIES_PER_TAG, None)
    df.to_csv('tmp/exp-npol-get-policies.csv')
    get_policies_times.clear()


    # #### Number of policies vs Number of schemas ####
    # res = np.zeros([len(N_SCHEMAS), len(N_POLICIES_PER_TAG)])

    # for npolicies in N_POLICIES_PER_TAG:
    #     current_experiment_variables = default_experiment_variables.copy()
    #     current_experiment_variables['-Ddatamanager.generatedata.npoliciespertag='] = npolicies

    #     for nschemas in N_SCHEMAS:
    #         current_experiment_variables['-Ddatamanager.generatedata.nschemas='] = nschemas
    #         current_experiment_args = fixed_experiment_args + [key + str(value) for key, value in current_experiment_variables.items()]
    #         name = f"exp-npol{npolicies}-nsch{nschemas}"
    #         avg_time = run_single_experiment(name, current_experiment_args)
    #         res[N_SCHEMAS.index(nschemas), N_POLICIES_PER_TAG.index(npolicies)] = avg_time

    # df = pd.DataFrame(res, index=N_SCHEMAS, columns=N_POLICIES_PER_TAG)
    # df.to_csv('tmp/exp-npol-nsch.csv')
    
    # df = process_log_times(matching_times, N_POLICIES_PER_TAG, N_SCHEMAS)
    # df.to_csv('tmp/exp-npol-nsch-matching.csv')
    # matching_times.clear()

    # df = process_log_times(get_policies_times, N_POLICIES_PER_TAG, N_SCHEMAS)
    # df.to_csv('tmp/exp-npol-nsch-get-policies.csv')
    # get_policies_times.clear()


run_processing_time_experiment()

stop(None, None)
