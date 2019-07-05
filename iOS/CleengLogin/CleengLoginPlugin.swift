//
//  CleengLoginPlugin.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/3/19.
//

import ZappPlugins
import ZappLoginPluginsSDK
import ApplicasterSDK
import CAM

private let kCleengUserLoginToken = "CleengUserLoginToken"

enum CleengErrors: Error {
    case authTokenNotParsed
}

@objc public class ZappCleengLogin: NSObject, ZPLoginProviderUserDataProtocol, ZPAppLoadingHookProtocol, ZPScreenHookAdapterProtocol {
    
    private var userToken: String?
    private var userPermissionEntitlementsIds = Set<String>()
    
    private var publisherId = ""
    private var networkAdapter: CleengNetworkHandler!
    public var configurationJSON: NSDictionary?
    
    private var flow: CAMFlow = .no
    
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
        networkAdapter.errorMessage = errorMessage()
    }
    
    public required init?(pluginModel: ZPPluginModel, screenModel: ZLScreenModel, dataSourceModel: NSObject?) {
        
    }
    
    public required init?(pluginModel: ZPPluginModel, dataSourceModel: NSObject?) {
        super.init()
        
        let playableItems = dataSourceModel as? [ZPPlayable] ?? []
        flow = FlowParser().parseFlow(from: playableItems)
    }
    
    // MARK: - Private methods
    
    private func parseEntitlements(from playableItems: [ZPPlayable]) -> [String] {
        let entitlementsKey = "ds_product_ids"
        return playableItems.first?.extensionsDictionary?[entitlementsKey] as? [String] ?? []
    }
    
    private func silentAuthorization(completion: @escaping (SilentLoginResult) -> Void) {
        guard let savedLoginToken = UserDefaults.standard.string(forKey: kCleengUserLoginToken) else {
            completion(.failure)
            return
        }
        
        networkAdapter.extendToken(token: savedLoginToken, completion: { (result) in
            switch result {
            case .success(let data):
                let isParsed = self.parseAuthTokensResponse(json: data)
                isParsed == true ? completion(.success) : completion(.failure)
            case .failure:
                completion(.failure)
            }
        })
    }
    
    private func authorize(api: CleengAPI, completion: @escaping (CAM.Result<Void>) -> Void) {
        networkAdapter.authorize(apiRequest: api, completion: { (result) in
            switch result {
            case .success(let data):
                let isParsed = self.parseAuthTokensResponse(json: data)
                isParsed == true ? completion(.success(())) : completion(.failure(CleengErrors.authTokenNotParsed))
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    private func errorMessage() -> (ErrorCodes) -> String {
        let nonexistentAlertKey = "nonexistent_user_alert_text"
        let existingUserAlertKey = "existing_user_alert_text"
        let invalidCredentialsAlertKey = "invalid_credentials_alert_text"
        let defaultAlertKey = "default_alert_text"
        
        let nonexistentAlertMessage = configurationJSON?[nonexistentAlertKey] as? String ?? ""
        let existingUserAlertMessage = configurationJSON?[existingUserAlertKey] as? String ?? ""
        let invalidCredentialsAlertMessage = configurationJSON?[invalidCredentialsAlertKey] as? String ?? ""
        let defaultAlertMessage = configurationJSON?[defaultAlertKey] as? String ?? ""
        
        let result: (ErrorCodes) -> String = { errorCode in
            var errorMessage = ""
            
            switch errorCode {
            case .nonexistentUser:
                errorMessage = nonexistentAlertMessage
            case .existingUser:
                errorMessage = existingUserAlertMessage
            case .invalidCredentials:
                errorMessage = invalidCredentialsAlertMessage
            case .unknown:
                errorMessage = defaultAlertMessage
            }
            
            return errorMessage
        }
        
        return result
    }
    
    // MARK: - ZPAppLoadingHookProtocol
    
    public func executeAfterAppRootPresentation(displayViewController: UIViewController?,
                                                completion: (() -> Swift.Void)?) {
        silentAuthorization(completion: { (result) in
            switch result {
            case .success:
                completion?()
            case .failure:
                self.executeAfterAppRootPresentationFlow(displayViewController: displayViewController,
                                                         completion: completion)
            }
        })
    }
    
    private func executeAfterAppRootPresentationFlow(displayViewController: UIViewController?,
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
    
    // MARK: - ZPLoginProviderUserDataProtocol
    
    public func isUserComply(policies: [String: NSObject]) -> Bool {
        let parser = FlowParser()
        let playableItems = parser.parsePlayableItems(from: policies)
        let flow = parser.parseFlow(from: playableItems)
        
        assert(playableItems.count == 1, "It is assumed only one item comes in this method.")
        
        var isComply = false
        
        switch flow {
        case .authentication:
            isComply = isAuthenticated()
        case .storefront:
            let entitlements = parseEntitlements(from: playableItems)
            isComply = !(userPermissionEntitlementsIds.isDisjoint(with: entitlements))
        case .authAndStorefront:
            if isAuthenticated() == true {
                let entitlements = parseEntitlements(from: playableItems)
                isComply = !(userPermissionEntitlementsIds.isDisjoint(with: entitlements))
            }
        case .no:
            isComply = true
        }
        
        return isComply
    }
 
    public func isUserComply(policies: [String: NSObject], completion: @escaping (Bool) -> Void) {
        let result = isUserComply(policies: policies)
        completion(result)
    }
    
    // MARK: - ZPLoginProviderProtocol
    
    public func login(_ additionalParameters: [String: Any]?,
                      completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
    
        guard let controller = UIViewController.topmostViewController() else {
            assert(false, "No topmost controller")
            completion(.failed)
        }
        
        var flow = self.flow
        
        if additionalParameters != nil {
            flow = FlowParser().parseFlow(from: additionalParameters)
        }
        
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
        return userToken != nil
    }
    
    public func isPerformingAuthorizationFlow() -> Bool {
        return networkAdapter.isPerformingAuthorizationFlow

    }
    
    public func getUserToken() -> String {
        return userToken ?? ""
    }
    
    // MARK: - JSON Response parsing
    
    private func parseAuthTokensResponse(json: Data) -> Bool {
        guard let cleengTokens = try? JSONDecoder().decode(CleengTokens.self, from: json) else {
            return false
        }
        for item in cleengTokens {
            if item.offerID.isEmpty {
                userToken = item.token // if offerID empty than we retrieve user token
                UserDefaults.standard.set(item.token, forKey: kCleengUserLoginToken)
            } else {
                if let authID = item.authID {
                    userPermissionEntitlementsIds.insert(authID) // if offerID !empty put subscription token in dicrionary by authId
                }
            }
        }
        return true
    }
    
    // MARK: - ZPScreenHookAdapterProtocol
    
    public func executeHook(presentationIndex: NSInteger,
                            dataDict: [String: Any]?,
                            taskFinishedWithCompletion: @escaping (Bool, NSError?, [String: Any]?) -> Void) {
        login(nil) { (operationStatus) in
            switch operationStatus {
            case .completedSuccessfully:
                taskFinishedWithCompletion(true, nil, nil)
            case .failed, .cancelled:
                taskFinishedWithCompletion(false, nil, nil)
            }
        }
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
    
    public func facebookLogin(userData: (email: String, userId: String),
                              completion: @escaping (LoginResult) -> Void) {
        let api = CleengAPI.loginWithFacebook(publisherID: publisherId,
                                              email: userData.email,
                                              facebookId: userData.userId)
//        authorize(api: api, completion: completion)
        authorize(api: api) { (result) in
            
        }
    }
    
    public func facebookSignUp(userData: (email: String, userId: String),
                               completion: @escaping (SignupResult) -> Void) {
        let api = CleengAPI.registerWithFacebook(publisherID: publisherId, email: userData.email,
                                                 facebookId: userData.userId)
        authorize(api: api, completion: completion)
    }
    
    public func login(authData: [String: String], completion: @escaping (LoginResult) -> Void) {
        let api = CleengAPI.login(publisherID: publisherId, email: authData["email"] ?? "",
                                  password: authData["password"] ?? "")
        authorize(api: api, completion: completion)
    }
    
    public func signUp(authData: [String: String], completion: @escaping (SignupResult) -> Void) {
        let api = CleengAPI.register(publisherID: publisherId, email: authData["email"] ?? "",
                                     password: authData["password"] ?? "")
        authorize(api: api, completion: completion)
    }
    
    public func resetPassword(data: [String: String], completion: @escaping (CAM.Result<Void>) -> Void) {
        networkAdapter.resetPassword(data: data, completion: { (result) in
            switch result {
            case .success:
                completion(CAM.Result.success(()))
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    public func availableProducts(completion: @escaping (AvailableProductsResult) -> Void) {
        completion(.success([]))
    }
    
    public func itemPurchased(purchasedItem: PurchasedProduct,
                              completion: @escaping (PurchaseResult) -> Void) {
        
    }
    
    public func itemsRestored(restoredItems: [PurchasedProduct],
                              completion: @escaping (PurchaseResult) -> Void) {
        
    }
}
