package com.citytechinc.aem.prosper.specs

import com.citytechinc.aem.prosper.adapter.AdapterFactoryHolder
import com.citytechinc.aem.prosper.annotations.ContentFilter
import com.citytechinc.aem.prosper.annotations.ContentFilterRule
import com.citytechinc.aem.prosper.annotations.ContentFilterRuleType
import com.citytechinc.aem.prosper.annotations.ContentFilters
import com.citytechinc.aem.prosper.annotations.NodeTypes
import com.day.cq.tagging.TagManager
import com.day.cq.wcm.api.Page
import com.day.cq.wcm.api.PageManager
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.adapter.AdapterFactory
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ValueMap
import spock.lang.Unroll

import javax.jcr.Node
import javax.jcr.Session

@Unroll
@ContentFilters(
    filters = [
        @ContentFilter(root = "/content", rules = [
            @ContentFilterRule(pattern = "/content/prosper(/.*)?", type = ContentFilterRuleType.INCLUDE),
            @ContentFilterRule(pattern = "/content/dam(/.*)?", type = ContentFilterRuleType.EXCLUDE)
        ]),
        @ContentFilter(root = "/etc")
    ]
)
@NodeTypes("/SLING-INF/nodetypes/spock.cnd")
class ProsperSpecSpec extends ProsperSpec {

    @Override
    Collection<AdapterFactoryHolder> addAdapterFactories() {
        def adapterFactory = new AdapterFactory() {
            @Override
            def <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
                def result

                if (adaptable instanceof Resource) {
                    result = type == Integer ? 1982 : null
                } else if (adaptable instanceof ResourceResolver) {
                    result = type == Integer ? 2014 : null
                } else {
                    result = null
                }

                (AdapterType) result
            }
        }

        def holder = new AdapterFactoryHolder(adapterFactory, [Resource.name, ResourceResolver.name] as String[],
            [Integer.name] as String[])

        [holder]
    }

    @Override
    Map<Class, Closure> addResourceAdapters() {
        [(String): { "hello" }]
    }

    @Override
    Map<Class, Closure> addResourceResolverAdapters() {
        [(String): { "world" }]
    }

    @Override
    Map<Class, Closure> addRequestAdapters() {
        [(String): { "!" }]
    }

    def setupSpec() {
        nodeBuilder.etc {
            spock("spock:TestType")
        }
    }

    def "registered adapter factory"() {
        expect:
        resourceResolver.adaptTo(Integer) == 2014
        resourceResolver.getResource("/").adaptTo(Integer) == 1982
    }

    def "adapt to page manager"() {
        expect:
        resourceResolver.adaptTo(PageManager)
    }

    def "adapt to tag manager"() {
        expect:
        resourceResolver.adaptTo(TagManager)
    }

    def "adapt to session"() {
        expect:
        resourceResolver.adaptTo(Session)
    }

    def "adapt to invalid type returns null"() {
        expect:
        !resourceResolver.adaptTo(Boolean)
    }

    def "additional resource adapter"() {
        expect:
        resourceResolver.getResource("/").adaptTo(String) == "hello"
    }

    def "additional resource resolver adapter"() {
        expect:
        resourceResolver.adaptTo(String) == "world"
    }

    def "additional request adapter"() {
        expect:
        requestBuilder.build().adaptTo(String) == "!"
    }

    def "add resource resolver adapter for test"() {
        setup:
        slingContext.registerAdapter(ResourceResolver, Map, { [:] })

        expect:
        resourceResolver.adaptTo(Map) == [:]
    }

    def "add request adapter for test"() {
        setup:
        slingContext.registerAdapter(SlingHttpServletRequest, Map, { [adapted: "request"] })

        expect:
        requestBuilder.build().adaptTo(Map) == [adapted: "request"]
    }

    def "adapt resource to page"() {
        setup:
        def resource = getResource("/content/prosper")

        expect:
        resource.adaptTo(Page)
    }

    def "adapt resource to value map"() {
        setup:
        def resource = getResource("/content/prosper")

        expect:
        resource.adaptTo(ValueMap)
    }

    def "adapt resource to node"() {
        setup:
        def resource = getResource("/content/prosper")

        expect:
        resource.adaptTo(Node)
    }

    def "check node type for node with custom type"() {
        expect:
        getNode("/etc/spock").isNodeType("spock:TestType")
    }

    def "verify test content was imported successfully"() {
        expect:
        getResource(path)

        where:
        path << [
            "/content/prosper",
            "/etc/designs/default"
        ]
    }

    def "verify excluded content was not imported"() {
        expect:
        !getResource("/content/dam")
    }
}
