//
//  CleengLoginPlugin.swift
//  Alamofire
//
//  Created by Egor Brel on 6/3/19.
//

import ZappPlugins
import ZappLoginPluginsSDK
import ApplicasterSDK
import CAM

@objc public class ZappCleengLogin: NSObject, ZPLoginProviderUserDataProtocol, ZPAppLoadingHookProtocol {
    
    /// Cleeng publisher identifier. **Required**
    private var publisherId = ""
    private var networkAdapter: CleengNetworkHandler!
    public var configurationJSON: NSDictionary?
    
    public required override init() {
        super.init()
        networkAdapter = CleengNetworkHandler(publisherID: "")
    }
    
    public required init(configurationJSON: NSDictionary?) {
        super.init()
        self.configurationJSON = configurationJSON
        if let id = configurationJSON?["cleeng_login_publisher_id"] as? String {
            publisherId = id
        }
        networkAdapter = CleengNetworkHandler(publisherID: publisherId)
    }
    
    // MARK: - Private methods
    
    private func parsePlayableItems(from dictionary: [String: Any]?) -> [ZPPlayable] {
        let playableItemsKey = "playable_items"
        let playableItems = dictionary?[playableItemsKey] as? [ZPPlayable] ?? []
        return playableItems
    }
    
    private func parseFlow(from playableItems: [ZPPlayable]) -> CAMFlow {
        // In general we assume only one item comes to plugin
        guard let item = playableItems.first else {
            assert(false)
            return .no
        }
        
        let authKey = "requires_authentication"
        let entitlementsKey = "ds_product_ids"
        
        let isAuthRequired = item.extensionsDictionary?[authKey] as? Bool ?? false
        let entitlements = item.extensionsDictionary?[entitlementsKey] as? [String] ?? []
        
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
    
    // MARK: - ZPAppLoadingHookProtocol
    
    public func executeAfterAppRootPresentation(displayViewController: UIViewController?,
                                                completion: (() -> Swift.Void)?) {
        guard let startOnAppLaunch = configurationJSON?["cleeng_login_start_on_app_launch"] else {
            completion?()
            return
        }
        var presentLogin = false
        if let flag = startOnAppLaunch as? Bool {
            presentLogin = flag
        } else if let num = startOnAppLaunch as? Int {
            presentLogin = (num == 1)
        } else if let str = startOnAppLaunch as? String {
            presentLogin = (str == "1")
        }
        
        if presentLogin {
            guard let controller = displayViewController else {
                completion?()
                return
            }
            let contentAccessManager = ContentAccessManager(rootViewController: controller,
                                                            camDelegate: self,
                                                            camFlow: .authentication,
                                                            completion: { _ in completion?() })
            contentAccessManager.startFlow()
        }
    }
   
    // MARK: - ZPLoginProviderUserDataProtocol`
 
    public func isUserComply(policies: [String: NSObject], completion: @escaping (Bool) -> Void) {
        completion(false)
    }
    
    public func login(_ additionalParameters: [String: Any]?,
                      completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
    
        guard let controller = UIViewController.topmostViewController() else {
            assert(false, "No topmost controller")
            completion(.failed)
        }
        
        let playableItems = parsePlayableItems(from: additionalParameters)
        let flow = parseFlow(from: playableItems)
        
        let contentAccessManager = ContentAccessManager(rootViewController: controller,
                                                        camDelegate: self,
                                                        camFlow: flow) { (isCompleted) in
            (isCompleted == true) ? completion(.completedSuccessfully) : completion(.failed)
        }
        contentAccessManager.startFlow()
    }
    
    public func logout(_ completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
        completion(.completedSuccessfully)
    }
    
    public func isAuthenticated() -> Bool {
        return false
    }
    
    public func isPerformingAuthorizationFlow() -> Bool {
        // TODO: Fix it
        return false

    }
    
    public func getUserToken() -> String {
        return ""
    }
}

// MARK: - CAMDelegate

extension ZappCleengLogin: CAMDelegate {
    
    public func getPluginConfig() -> [String: String] {
        if let config = configurationJSON as? [String: String] {
            return config
        }
        return [String: String]()
    }
    
    public func isUserLogged() -> Bool {
        return false
    }
    
    public func isPurchaseNeeded() -> Bool {
        return true
    }
    
    public func facebookLogin(userData: (email: String, userId: String), completion: @escaping (CAMResult) -> Void) {
        
    }
    
    public func facebookSignUp(userData: (email: String, userId: String), completion: @escaping (CAMResult) -> Void) {
        
    }
    
    public func login(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        networkAdapter.login(authData: authData, completion: { (_) in
            
        })
    }
    
    public func signUp(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        
    }
    
    public func resetPassword(data: [String: String], completion: @escaping (CAMResult) -> Void) {
        
    }
    
    public func itemPurchased(item: SKProduct) {
        
    }
    
    public func itemsRestored(items: [SKPaymentTransaction]) {
        
    }
    
    public func availableProducts() -> [Product] {
        return [Product]()
    }
}
