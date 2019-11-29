//
//  ItemAccessChecker.swift
//  CleengLogin
//
//  Created by Egor Brel on 7/9/19.
//

import Foundation
import CAM
import ZappPlugins

class AccessChecker {
    private var flowParser = FlowParser()
    static var userPermissionEntitlementsIds = Set<String>()
    var currentItemEntitlementsIds = [String]() //Auth Ids from dsp
    var isPurchaseNeeded: Bool {
        return AccessChecker.userPermissionEntitlementsIds.isDisjoint(with: currentItemEntitlementsIds)
    }
    
    public func isUserComply(policies: [String: NSObject], isAuthenticated: Bool) -> Bool {
        let flow = flowParser.parseFlow(from: policies)
        
        var isComply = false
        
        switch flow {
        case .authentication:
            isComply = isAuthenticated
        case .storefront:
            let entitlements = flowParser.parseEntitlements(from: policies)
            isComply = !(AccessChecker.userPermissionEntitlementsIds.isDisjoint(with: entitlements))
        case .authAndStorefront:
            if isAuthenticated {
                let entitlements = flowParser.parseEntitlements(from: policies)
                isComply = !(AccessChecker.userPermissionEntitlementsIds.isDisjoint(with: entitlements))
            }
        case .no:
            isComply = true
        default:
            break
        }
        
        return isComply
    }
    
    public func getStartupFlow(for dictionary: [String: Any]?) -> CAMFlow {
        var isTriggerOnAppLaunch = false
        if let startOnAppLaunch = dictionary?["trigger_on_app_launch"] {
            if let flag = startOnAppLaunch as? Bool {
                isTriggerOnAppLaunch = flag
            } else if let num = startOnAppLaunch as? Int {
                isTriggerOnAppLaunch = (num == 1)
            } else if let str = startOnAppLaunch as? String {
                isTriggerOnAppLaunch = (str == "1")
            }
        }
        
        var shouldPresentStorefront = false
        
        if let storefrontOnAppLaunch = dictionary?["present_storefront_upon_launch"] {
            if let flag = storefrontOnAppLaunch as? Bool {
                shouldPresentStorefront = flag
            } else if let num = storefrontOnAppLaunch as? Int {
                shouldPresentStorefront = (num == 1)
            } else if let str = storefrontOnAppLaunch as? String {
                shouldPresentStorefront = (str == "1")
            }
        }
        
        if let startupAuthIDs = dictionary?["app_level_product_ids"] as? String, shouldPresentStorefront {
            let ids = startupAuthIDs.split(separator: ",").map(String.init)
            if !ids.isEmpty {
                shouldPresentStorefront = true
                setItemAuthIDs(from: ids)
            } else {
                shouldPresentStorefront = false
            }
        } else {
            shouldPresentStorefront = false
        }
        
        switch (isTriggerOnAppLaunch, shouldPresentStorefront) {
        case (true, false):
            return .authentication
        case (true, true):
            return .authAndStorefront
        case (false, false):
            return .no
        case (false, true):
            return .storefront
        }
    }
    
    public func getCamFlow(for dictionary: [String: Any]) -> CAMFlow {
        let authIds = flowParser.parseEntitlements(from: dictionary)
        setItemAuthIDs(from: authIds)
        let flow = flowParser.parseFlow(from: dictionary)
        return flow
    }
    
    private func setItemAuthIDs(from authIDs: [String]) {
        currentItemEntitlementsIds.removeAll()
        currentItemEntitlementsIds.append(contentsOf: authIDs)
    }
}
