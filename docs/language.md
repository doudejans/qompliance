# Policy model & language

This document describes the YAML implementation of the policy model.
The YAML implementation serves as a policy language and is used as the format for submitting policies to the system.
This specification uses the example attributes used in the thesis, but can be trivially extended to support other attributes.

## Specification

A policy consists of the following basic elements:

- A unique name
- Context attributes that define the applicability of a policy
- A decision about whether the transformation/movement on which the policy applies is allowed or not
- Requirement attributes that dictate what should happen if a policy applies on the request (only allowed for `allow` and `nondeciding` policies)

Attributes can have different types which in turn have their own semantics.
In the following sections, we will discuss these basic elements, and their supported attributes.

### Name (required)

Every policy should have a name that can be used as a natural identifier, for referring to the policy in other places or for example in (audit) logs.
This name should be unique.

Syntax:

```yaml
name: <name>
```

### Context (required)

The 'context' section of a policy lists all context conditions that should hold for a policy to be applicable.
It can contain the following attributes: tag, role, purpose, data location and storage classification.
A policy does not have to specify all attributes.
Between the attributes in the context themselves there is a logical conjunction (AND): they all have to apply in some way for a policy to apply.
Between attribute values for all attributes there is a logical disjunction (OR): any value has to apply on the input for the attribute to apply.
For example, if a policy specifies tags and purposes, some tags and some purposes of the policy should be applicable on the input for the policy to be applicable on the input.

Note that exact semantics of what constitutes an attribute 'match' can differ between attribute types.
Especially if a policy lists multiple attribute values per attribute, this introduces the challenge of how to consider these multiple values (e.g., all values should apply or at least one).
Theoretically, the system supports defining attribute types which use different semantics when determining a match.
However, the context attribute types in this proposed model only really consider the attribute values using OR-semantics.
In short, this means that **for all attributes in the policy, at least one value should apply on the input**.

Syntax:

```yaml
context:
  tag:
  role:
  purpose:
  data-location:
  storage-classification:
```

#### Tag

The most powerful contextual attribute in the system are tags.
They play a central role in the data-centric approach of this system.
Because of this, they have a few special traits that other attributes do not have (and they are treated a bit differently internally), more on this later.

Tags are used for restricting what data a policy should apply to.
Tags serve as a layer of abstraction between the 'low-level' data model of the data that the system is governing, and the 'high-level' policies.
This decouples the data definition from policies, and together with the implied meaning of the tags it allows the policy author to write declarative policies.
For this to work well, all input data for the transformation has to be tagged with predefined tags that can apply to datastore(s), table(s) and column(s).

The decision to use tags comes with a number of benefits and drawbacks.
We will list some here which should be taken into account when implementing the system.

Benefits include:

- **Flexible:** Tags can apply at any level of detail, both the meaning of a tag and the level at which data is tagged (datastore, table, column).
- **Decoupling policy from data definition:** Policies do not directly reference the schema of the governed data (like in many other systems).
  This means that new schemas can easily be added to the system without rewriting policies, and the other way around.
- **Easier to understand policies:** Because of this decoupling, policy authors can write more declarative policies by using the meaning of the tags, all without knowledge of the data in the system.
  Policies will also be easier to understand for people unfamiliar with the data at all: a policy that simply references a 'PII' tag will be easier to understand than a policy referencing all columns that are considered PII.
- **Tags/metadata are common:** The use of tags and metadata in general is well-established in the industry.
  Many systems use metadata and tags for managing data, and a lot of research has been done towards managing and proactively generating metadata (which can be useful for this system as well).
  The tags in this system can also be used by other systems or the other way around.
  
Drawbacks include:

- **Picking the right tags:** Assigning the right tags can be difficult, especially if there are many tags, and there is more potential for conflict.
- **Adds a layer of abstraction:** Using tags creates an implicit layer between the policies and the data which could make it less obvious to see what policies apply on what data.
  This might influence the 'effectiveness' of the compliance system (i.e., whether all data that should be governed by a policy is in fact linked to this policy so that it can be checked).
  Moreover, these tags have to actually be assigned, either automatically or by someone with knowledge about the data.
  However, a clear user interface for managing this layer could help with managing this problem.
- **No support for direct references in policies:** In some cases, it could be powerful to write very specific policies with highly specific conditions on the data.
  By not supporting direct references to the data, the policy model loses some power and expressiveness.
  However, with specific enough tags these references can technically be emulated if desired.

Tags have to be predefined in the system, and data has to be tagged before the policies can be enforced.
Tags are stored in a hierarchy, meaning that tags can have parents and children.
This hierarchy can be used to organize the tags, and is also used for conflict resolution.
An important difference to note is that this hierarchy of tags does not imply that if a certain tag is used in a policy, its children are also used for matching policies.
This is an exception because for all other hierarchical attributes, this is in fact the case.
This decision was made because it makes policies more understandable.
With attributes like a geolocation, it is very clear that if a country is included in a policy, all cities in this country are implicitly also included.
However, with tags this behavior can quickly get confusing, thus we opted to require exact tag matches between policies and the tagged data.

Remember that, as with all context attribute values, tags in policies always have an OR relationship meaning that it is considered to be a match for the tag attribute if any of the tags apply.

Syntax:

```yaml
tag:
  - <tag>
```

#### Role

The role of a user is used for simple role-based access control, which is a typical feature in many access control systems.
This is a hierarchical attribute, meaning that the finite set of role names should be predefined in a tree, where roles can also inherit from other roles.
These role names can for example be mapped from LDAP groups.

The syntax for specifying roles:

```yaml
role:
  - <role>
```

This attribute is optional.
If roles are not included in a policy, the policy applies to all roles.

#### Purpose

The submitter of the job should specify the purpose for the transformation (from a fixed hierarchy of options).
This is a hierarchical attribute, meaning that the finite set of purposes should be predefined in a tree, where purposes can also inherit from other purposes.

The syntax for specifying purposes:

```yaml
purpose:
  - <purpose>
```

This attribute is optional.
If purposes are not included in a policy, the policy applies to all purposes.

#### Data location

The data location is another important attribute in the system, which enables the geolocation features of this system.
It allows policy authors to write policies that govern how data should be managed across different borders and in different jurisdictions.
Together with the data location requirement attribute, it can also be used to 'steer' data to the right location.

This is again a hierarchical attribute, meaning that the locations have to be predefined in a tree.
Implicitly, these trees can be based on the hierarchy of locations, e.g., Amsterdam is in the Netherlands, which is in Europe.
Syntax:

```yaml
data-location:
  - <data-location>
```

This attribute is optional.
If data locations are not included in a policy, the policy applies to all locations.

#### Storage classification

The storage classification context attribute can be added to a policy to restrict on what types of data stores a policy applies on.
The classifications are labels that are attached to data stores in the metadata.
Classifications can for example be useful to select data stores that have a particular compliance certification, are disk encrypted or are permitted to store sensitive data.
This attribute's requirement counterpart can be used to 'steer' data to a data store with a certain classification.

Storage classifications have an enum data type, meaning that all possible values are a simple predefined list of values.

Syntax:

```yaml
storage-classification:
  - <storage-classification>
```

This attribute is optional.
If storage classifications are not included in a policy, the policy applies to all storage classifications.

### Decision (required)

A decision determines whether the policy allows the input to be processed or not, given that the policy context applies on the input.
This is comparable to and derived from traditional access control where this would control whether someone has access to the data or not.
However, because these decisions are not enforced at data access but rather at the point of data transformation or movement, this decision should be interpreted a bit differently.
This can still enable 'traditional' access control if all data access is required to go through SQL queries via our system.

A decision can be one of: `allow`, `deny`, `nondeciding`.

A `nondeciding` policy is a policy which does not make an actual decision about whether a policy allows a certain input.
The main benefit of a `nondeciding` policy is that it still allows the policy to specify requirements.
A `deny` policy cannot specify requirements because since the transformation is not allowed, requirements cannot be enforced anyways.
One could for example use a `nondeciding` policy to put requirements on the data location, without attaching this to decisions about specific data.
A `nondeciding` policy does not influence the final decision and lets other policies determine the decision outcome.
If all policies are `nondeciding`, the system's (configurable) default decision is used.

Syntax:

```yaml
decision: <allow/deny/nondeciding>
```

Note that although every policy has to have one of the three decision values, a policy in the YAML implementation does not have to explicitly list a decision.
In this case, the decision will be set to `nondeciding` since this naturally follows from how you would read such a policy.

### Require

The 'require' section of a policy lists all requirements that are required to be enforced once a policy is determined to be applicable on the input (based on the policy's context).
A policy should either have decision `allow` or `nondeciding` to be allowed to list requirements.
It can contain the following requirement attributes: data locations, storage classifications, without and aggregate.
Requirements are entirely optional, meaning that a policy does not have to specify all attributes or any requirements at all.
Requirement attributes all have to be enforceable for a policy to be considered valid and applicable.
If a policy matches with a certain input and the requirements are validated, but the system determines that they cannot be enforced in some way, the policy decision will be set to `indeterminate`.

Note that the semantics of the requirements and what constitutes a valid requirement can vary between requirement types and what operations they enforce.
Conflict resolution, handling multiple attribute values and the semantical meaning of the output of a requirement can also differ per requirement.

Syntax:

```yaml
require:
  data-location:
  storage-classification:
  without:
  aggregate:
```

#### Data location

Together with the data location context attribute, this requirement enables the geolocation features of this system.
It allows policy authors to write policies that govern where data is being processed and stored.

This attribute has the same properties as its context counterpart, meaning that the values use the same predefined tree.

Syntax:

```yaml
data-location:
  - <data-location>
```

#### Storage classification

The storage classification requirement is another powerful requirement which can be used to dictate where data can be stored.
The storage classifications are labels that are attached to data stores in the metadata.
These can be used to select data stores that conform to a particular classification.
Classifications can for example be useful to select data stores that have a particular compliance certification, are disk encrypted or are permitted to store sensitive data.

Storage classifications have an enum data type, meaning that all possible values are a simple predefined list of values.

```yaml
storage-classification:
  - <data-location>
```

#### Without

The 'without' requirement can be used to mandate that data is not accessed in the transformation and is not included in the final result, and thus the final SQL.
This is useful to write policies that restrict what data can be accessed without preventing the data access entirely.
It can be used to prevent data from being queried/joined and stored together.
The possible values of this attribute are the set of tags that are registered in the system.
This requirement is only considered to be satisfied if all tags mentioned in the policy do not reference any data touched by the SQL.

The requirement operates in the following way: first the tag is resolved to see what data it applies on.
Then, the query is being checked for any references to the data in this set.
If any of this data is included, a rewritten query suggestion will be made to the user.
The input will not be allowed until this requirement is satisfied, either by rewriting the query or by accepting the query suggestion.

```yaml
without:
  - <tag>
```

#### Aggregate

The aggregate requirement is used similarly to the 'without' requirement, but instead only allows a SQL query where the referenced data has been aggregated (or not present at all).
This is another way to restrict what data ends up being used, but is a bit more lenient than entirely excluding the data from a column.
The possible values of this attribute are the set of tags that are registered in the system.
This requirement is considered to be satisfied if all tags mentioned in the policy refer to data that is either in the SQL as aggregated columns or not in the SQL at all.

The requirement operates in the following way: first the tag is resolved to see what data it applies on.
The set of data references to columns that are actually in the query and need to be aggregated according to the policy is the intersection between the data references in the query and in the set of resolved tags.
Then we subtract the set of data references in the query that are in an aggregation function.
If there is any column references left, these have not been aggregated and thus the user needs to update the query to reflect this requirement.

```yaml
aggregate:
  - <tag>
```

## Examples

In this section we show various interesting examples to demonstrate how the policy model and language works.

```yaml
name: Keep data from leaving the EU
context:
  data-location:
    - EU
require:
  data-location:
    - EU
```

```yaml
name: Marketing department can access customer data given that PII is removed
context:
  tag:
    - customer_data
  role:
    - Marketing Dept
decision: allow
require:
  without:
    - PII
```

```yaml
name: Medical information should be kept in HIPAA compliant storage
context:
  tag:
    - medical
require:
  storage-classification:
    - HIPAA
```

```yaml
name: Data scientists can access all data for specific purposes
context:
  tag:
    - sales_data
    - customer_data
    - financial_data
  purpose:
    - analytics
    - research
  role:
    - Data Science Dept
decision: allow
require:
  without:
    - PII
```

```yaml
name: Customer data cannot be joined with credit card data
context:
  tag:
    - customer_data
require:
  without:
    - credit_card_data
```
