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
        if let startupAuthIDs = dictionary?["app_level_product_ids"] as? String {
            let ids = startupAuthIDs.split(separator: ",").map(String.init)
            if !ids.isEmpty {
                shouldPresentStorefront = true
                setItemAuthIDs(from: ids)
            }
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
    
    public func getCamFlow(for dictionary: [String: Any]?) -> CAMFlow {
        guard let dictionary = dictionary else {
            return .no
        }
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
