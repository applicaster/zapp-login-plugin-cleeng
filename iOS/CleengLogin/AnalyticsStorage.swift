//
//  AnalyticsStorage.swift
//  CleengLogin
//
//  Created by Roman Karpievich on 11/18/19.
//

import Foundation
import CAM
import ZappPlugins

public class AnalyticsStorage: AnalyticsStorageProtocol {
    public var trigger: Trigger = .appLaunch
    private(set) public var itemName: String = Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String ?? ""
    private(set) public var itemType: String = "App"
    private(set) public var purchasesProperties: [String: PurchaseProperties] = [:]
    
    func updatePurchasesProperties(from offers: [CleengOffer]) {
        let isSubsriber = AccessChecker.userPermissionEntitlementsIds.isEmpty == false
        
        var properties: [String: PurchaseProperties] = [:]
        
        offers.forEach { offer in
            var purchaseProperties = PurchaseProperties(productIdentifier: offer.appleProductID,
                                                        isSubscriber: isSubsriber)
            
            purchaseProperties.trialPeriod = offer.freeDays
            
            if let period = offer.period, period.isEmpty == false {
                purchaseProperties.subscriptionDuration = period
            }
            
            if let tags = offer.accessToTags, tags.isEmpty == false {
                purchaseProperties.purchaseEntityType = .category
            } else {
                purchaseProperties.purchaseEntityType = .vod
            }
            
            properties[offer.appleProductID] = purchaseProperties
        }
        
        purchasesProperties = properties
    }
    
    func updateProperties(from playableItem: ZPPlayable) {
        itemName = playableItem.playableName()
        
        if let isPlaylist = playableItem.isPlaylist, isPlaylist == true {
            itemType = "Feed"
        } else {
            itemType = "Video"
        }
    }
    
    func updatePropertiesForUserAccountComponent() {
        trigger = .userAccountComponent
        itemType = "UserAccounts Component"
    }
}
