/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package im.xz.cn.authorization;

import java.util.Collection;


public final class AccessPolicy {

    
    public static final String STATE_CANCELLED = "user.state.cancelled";
    public static final String STATE_ACCESSIBLE = "user.accessible";

    private AccessPolicy() {
    }

    
    public static boolean grantsCapability(Collection<String> grantedNodes, String capability) {
        if (capability == null || capability.isBlank()) {
            return false;
        }
        
        if (isStateNode(capability)) {
            return false;
        }
        if (grantedNodes == null || grantedNodes.isEmpty()) {
            return false;
        }
        for (String granted : grantedNodes) {
            if (matchesCapability(granted, capability)) {
                return true;
            }
        }
        return false;
    }

    
    public static boolean isStateNode(String node) {
        return node != null && node.contains(".state.");
    }

    public static boolean grantsCapability(Collection<String> grantedNodes, String capability, String wildcard) {
        return grantsCapability(grantedNodes, capability) || (wildcard != null && grantedNodes != null
                && grantedNodes.contains(wildcard));
    }

    
    public static boolean hasState(Collection<String> grantedNodes, String stateNode) {
        if (stateNode == null || stateNode.isBlank() || grantedNodes == null) {
            return false;
        }
        for (String granted : grantedNodes) {
            if (granted != null && granted.equals(stateNode)) {
                return true;
            }
        }
        return false;
    }

    
    public static boolean matchesCapability(String grantedNode, String capability) {
        if (grantedNode == null || grantedNode.isBlank() || capability == null) {
            return false;
        }
        String granted = grantedNode.trim();
        if ("*".equals(granted)) {
            return true;
        }
        if (granted.endsWith(".*")) {
            return capability.startsWith(granted.substring(0, granted.length() - 1));
        }
        return granted.equals(capability);
    }
}
