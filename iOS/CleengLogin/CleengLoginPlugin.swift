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

@objc public class ZappCleengLogin : NSObject, ZPLoginProviderUserDataProtocol, ZPAppLoadingHookProtocol {
    
    /// Cleeng publisher identifier. **Required**
    private var publisherId = ""
    private var contentAccessManager: ContentAccessManager?
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
    
    //MARK: - ZPAppLoadingHookProtocol
    
    public func executeAfterAppRootPresentation(displayViewController: UIViewController?, completion: (() -> Swift.Void)?) {
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
            contentAccessManager = ContentAccessManager(rootViewController: controller, camDelegate: self, completion: { [weak self] _ in
                self?.contentAccessManager = nil
                completion?()
            })
            contentAccessManager?.startFlow()
        }
    }
    
   
    //MARK: - ZPLoginProviderUserDataProtocol`
 
    public func isUserComply(policies: [String : NSObject], completion: @escaping (Bool) -> ()) {
        completion(false)
    }
    
    public func login(_ additionalParameters: [String : Any]?, completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
        completion(.completedSuccessfully)
    }
    
    public func logout(_ completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
        completion(.completedSuccessfully)
    }
    
    public func isAuthenticated() -> Bool {
        return false
    }
    
    public func isPerformingAuthorizationFlow() -> Bool {
        if let _ = contentAccessManager {
            return true
        }
        return false
    }
    
    public func getUserToken() -> String {
        return ""
    }
}

//MARK: - CAMDelegate

extension ZappCleengLogin: CAMDelegate {
    
    public func getPluginConfig() -> [String : String] {
        if let config = configurationJSON as? [String : String] {
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
