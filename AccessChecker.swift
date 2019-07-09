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
    var flowParser = FlowParser()
    var userPermissionEntitlementsIds = Set<String>()
    var currentVideoEntitlementsIds = [String]() //Auth Ids from dsp
    
    public func isUserComply(policies: [String: NSObject], isAuthenticated: Bool) -> Bool {
        let playableItems = flowParser.parsePlayableItems(from: policies)
        let flow = flowParser.parseFlow(from: playableItems)
        
        assert(playableItems.count == 1, "It is assumed only one item comes in this method.")
        
        var isComply = false
        
        switch flow {
        case .authentication:
            isComply = isAuthenticated
        case .storefront:
            let entitlements = parseEntitlements(from: playableItems)
            isComply = !(userPermissionEntitlementsIds.isDisjoint(with: entitlements))
        case .authAndStorefront:
            if isAuthenticated {
                let entitlements = parseEntitlements(from: playableItems)
                isComply = !(userPermissionEntitlementsIds.isDisjoint(with: entitlements))
            }
        case .no:
            isComply = true
        }
        
        return isComply
    }
    
    public func getStartupFlow(for dictionary: [String: Any]?, isAuthenticated: Bool) -> CAMFlow {
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
        if let startupAuthIDs = dictionary?["present_storefront_upon_launch"] as? String {
            let ids = startupAuthIDs.split(separator: ",").map(String.init)
            if !ids.isEmpty {
                shouldPresentStorefront = true
                setAuthIDs(from: ids)
            }
        }
        switch (isAuthenticated, isTriggerOnAppLaunch, shouldPresentStorefront) {
        case (false, true, false):
            return .authentication
        case (false, true, true):
            return .authAndStorefront
        case (true, true, true):
            return isPurchaseNeeded() ? .storefront : .no
        default:
            return .no
        }
    }
    
    public func getLoginFlow(for dictionary: [String: Any]?, isAuthenticated: Bool) -> CAMFlow {
        setAuthIDs(from: dictionary)
        let flow = flowParser.parseFlow(from: dictionary)
        switch flow {
        case .authentication:
            return isAuthenticated ? .no : .authentication
        case .authAndStorefront:
            let updatedFlow = isAuthenticated ? CAMFlow.storefront : CAMFlow.authAndStorefront
            if updatedFlow == .storefront {
                return isPurchaseNeeded() ? .storefront : .no
            }
            return updatedFlow
        case .storefront:
            return isPurchaseNeeded() ? .storefront : .no
        default:
            return flow
        }
    }
    
    public func isPurchaseNeeded() -> Bool {
        return userPermissionEntitlementsIds.isDisjoint(with: currentVideoEntitlementsIds)
    }
    
    private func setAuthIDs(from authIDs: [String]) {
        currentVideoEntitlementsIds.removeAll()
        currentVideoEntitlementsIds.append(contentsOf: authIDs)
    }
    
    private func setAuthIDs(from dictionary: [String: Any]?) {
        let playableItems = flowParser.parsePlayableItems(from: dictionary)
        let ids = parseEntitlements(from: playableItems)
        setAuthIDs(from: ids)
    }
    
    private func parseEntitlements(from playableItems: [ZPPlayable]) -> [String] {
        let entitlementsKey = "ds_product_ids"
        return playableItems.first?.extensionsDictionary?[entitlementsKey] as? [String] ?? []
    }
}
