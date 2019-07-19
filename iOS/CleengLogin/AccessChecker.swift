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
    var currentItemEntitlementsIds = [String]() //Auth Ids from dsp
    
    public func isUserComply(policies: [String: NSObject], isAuthenticated: Bool) -> Bool {
        let flow = flowParser.parseFlow(from: policies)
        
        var isComply = false
        
        switch flow {
        case .authentication:
            isComply = isAuthenticated
        case .storefront:
            let entitlements = flowParser.parseEntitlements(from: policies)
            isComply = !(userPermissionEntitlementsIds.isDisjoint(with: entitlements))
        case .authAndStorefront:
            if isAuthenticated {
                let entitlements = flowParser.parseEntitlements(from: policies)
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
                setItemAuthIDs(from: ids)
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
    
    public func getCamFlow(for dictionary: [String: Any]?, isAuthenticated: Bool) -> CAMFlow {
        guard let dictionary = dictionary else {
            return .no
        }
        let authIds = flowParser.parseEntitlements(from: dictionary)
        setItemAuthIDs(from: authIds)
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
        return userPermissionEntitlementsIds.isDisjoint(with: currentItemEntitlementsIds)
    }
    
    private func setItemAuthIDs(from authIDs: [String]) {
        currentItemEntitlementsIds.removeAll()
        currentItemEntitlementsIds.append(contentsOf: authIDs)
    }
}
