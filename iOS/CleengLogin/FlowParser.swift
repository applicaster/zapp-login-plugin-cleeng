//
//  FlowParser.swift
//  CleengLogin
//
//  Created by Roman Karpievich on 7/1/19.
//

import Foundation
import CAM
import ZappPlugins

private enum FlowParserKeys: String {
    case auth = "requires_authentication"
    case entitlements = "ds_product_ids"
    case playableItems = "playable_items"
    case vodItems = "vod_item_id"
    case legacyEntitlements = "authorization_providers_ids" // used to retrieve auth ids of legacy item, if not empty auth and purchase required
}

class FlowParser {
    
    // MARK: - Public methods
    
    public func parseFlow(from dictionary: [String: Any]?) -> CAMFlow {
        guard let dictionary = dictionary else {
            return .no
        }
        var isAuthRequired: Bool
        var entitlements: [String]
        
        if let legacyEntitlements = dictionary[FlowParserKeys.legacyEntitlements.rawValue] as? [Int] {
            isAuthRequired = !legacyEntitlements.isEmpty
            entitlements = legacyEntitlements.map { String($0) }
        } else {
            isAuthRequired = dictionary[FlowParserKeys.auth.rawValue] as? Bool ?? false
            entitlements = dictionary[FlowParserKeys.entitlements.rawValue] as? [String] ?? []
        }
        
        switch (isAuthRequired, entitlements.isEmpty) {
        case (true, true):
            return .authentication
        case (true, false):
            return .authAndStorefront
        case (false, false):
            return .storefront
        case (false, true):
            return .no
        }
    }
    
    public func parseEntitlements(from dictionary: [String: Any]) -> [String] {
        if let legacyEntitlements = dictionary[FlowParserKeys.legacyEntitlements.rawValue] as? [Int] {
            return legacyEntitlements.map { String($0) }
        } else {
            return dictionary[FlowParserKeys.entitlements.rawValue] as? [String] ?? []
        }
    }
}
