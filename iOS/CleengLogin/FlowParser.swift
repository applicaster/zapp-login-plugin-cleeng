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
        let playableItems = self.parsePlayableItems(from: dictionary)
        let flow = self.parseFlow(from: playableItems)
        
        return flow
    }
    
    public func parseFlow(from playableItems: [ZPPlayable]) -> CAMFlow {
        // In general we assume only one item comes to plugin
        guard let item = playableItems.first else {
            assert(false)
            return .no
        }
        var isAuthRequired: Bool
        var entitlements: [String]
        
        if let legacyEntitlements = item.extensionsDictionary?[FlowParserKeys.legacyEntitlements.rawValue] as? [String] {
            isAuthRequired = !legacyEntitlements.isEmpty
            entitlements = legacyEntitlements
        } else {
            isAuthRequired = item.extensionsDictionary?[FlowParserKeys.auth.rawValue] as? Bool ?? false
            entitlements = item.extensionsDictionary?[FlowParserKeys.entitlements.rawValue] as? [String] ?? []
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
    
    public func parsePlayableItems(from dictionary: [String: Any]?) -> [ZPPlayable] {
        var playableItems = dictionary?[FlowParserKeys.playableItems.rawValue] as? [ZPPlayable]
        if playableItems == nil {
            playableItems = dictionary?[FlowParserKeys.vodItems.rawValue] as? [ZPPlayable]
        }
        
        return playableItems ?? []
    }    
}
