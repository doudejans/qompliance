package tree

import com.qompliance.util.tree.AttributeValueTree
import com.qompliance.util.tree.TreeNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttributeValueTreeTest {
    private lateinit var tree: AttributeValueTree<String>

    @BeforeEach
    fun init() {
        val root = TreeNode("All")

        val marketing = TreeNode("Marketing")
        val research = TreeNode("Research")

        val advertising = TreeNode("Advertising")
        val audience = TreeNode("AudienceResearch")

        root.add(marketing)
        root.add(research)

        marketing.add(advertising)
        research.add(audience)

        tree = AttributeValueTree(root)
    }

    @Test
    fun searchLeafTest() {
        val actual = tree.search("Advertising")
        val expected = listOf("Advertising", "Marketing", "All")
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun searchMiddleNodeTest() {
        val actual = tree.search("Marketing")
        val expected = listOf("Marketing", "All")
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun searchRootTest() {
        val actual = tree.search("All")
        val expected = listOf("All")
        Assertions.assertEquals(expected, actual)
    }
}