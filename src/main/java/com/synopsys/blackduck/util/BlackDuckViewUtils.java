package com.synopsys.blackduck.util;

import com.synopsys.integration.blackduck.api.core.BlackDuckView;
import com.synopsys.integration.blackduck.api.core.ResourceLink;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;

/**
 * Author: dnichol - David Nicholls - Black Duck Solution Architect
 */
public class BlackDuckViewUtils {

    public static String getId(BlackDuckView view) {
        return getId(view.getMeta().getHref().string());
    }

    public static String getId(String id) {
        id = id.substring(id.lastIndexOf("/") + 1);
        return id;
    }

    public static void setHref(BlackDuckView view, String href) throws IntegrationException {
        if (view.getResourceMetadata() != null) {
            view.getResourceMetadata().setHref(new HttpUrl(href));
        }
    }

    public static ResourceLink getResourceLink(BlackDuckView view, String name) {
        if (view != null && name != null && view.getResourceLinks() != null) {
            for (ResourceLink link : view.getResourceLinks()) {
                if (name.equals(link.getRel())) {
                    return link;
                }
            }
        }
        return null;
    }
}
