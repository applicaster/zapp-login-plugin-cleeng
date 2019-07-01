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
    }
    
    public required init?(pluginModel: ZPPluginModel, screenModel: ZLScreenModel, dataSourceModel: NSObject?) {
        
    }
    
    public required init?(pluginModel: ZPPluginModel, dataSourceModel: NSObject?) {
        super.init()
        
        let playableItems = dataSourceModel as? [ZPPlayable] ?? []
        flow = parseFlow(from: playableItems)
    }
    
    // MARK: - Private methods
    
    private func parsePlayableItems(from dictionary: [String: Any]?) -> [ZPPlayable] {
        let playableItemsKey = "playable_items"
        let vodItemsKey = "vod_item_id"
        
        var playableItems = dictionary?[playableItemsKey] as? [ZPPlayable]
        if playableItems == nil {
            playableItems = dictionary?[vodItemsKey] as? [ZPPlayable]
        }
        
        return playableItems ?? []
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
                self.parseAuthTokensResponse(json: data, completion: { (result) in
                    result ? completion(.success) : completion(.failure)
                })
            case .failure:
                completion(.failure)
            }
        })
    }
    
    private func authorize(api: CleengAPI, completion: @escaping (CAMResult) -> Void) {
        networkAdapter.authorize(apiRequest: api, completion: { (result) in
            switch result {
            case .success(let data):
                self.parseAuthTokensResponse(json: data, completion: { (result) in
                    result ? completion(.success) : completion(.failure(description: "Server Error"))
                })
            case .failure(let error):
                self.camErrorWrapper(error: error, completion: completion)
            }
        })
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
        let playableItems = parsePlayableItems(from: policies)
        let flow = parseFlow(from: playableItems)
        
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
        return userToken != nil
    }
    
    public func isPerformingAuthorizationFlow() -> Bool {
        return networkAdapter.isPerformingAuthorizationFlow

    }
    
    public func getUserToken() -> String {
        return userToken ?? ""
    }
    
    // MARK: - JSON Response parsing
    
    private func parseAuthTokensResponse(json: Data, completion: (Bool) -> Void) {
        guard let cleengTokens = try? JSONDecoder().decode(CleengTokens.self, from: json) else {
            completion(false)
            return
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
        completion(true)
    }
    
    private func parseErrorResponse(json: Data, completion: (ServerError?) -> Void) {
        guard let serverError = try? JSONDecoder().decode(ServerError.self, from: json) else {
            completion(nil)
            return
        }
        completion(serverError)
    }
    
    private func camErrorWrapper(error: CleengError, completion: @escaping (CAMResult) -> Void) {
        switch error {
        case .serverError:
            completion(.failure(description: "Server Error"))
        case .requestError(let data):
            self.parseErrorResponse(json: data, completion: { (error) in
                guard let error = error else {
                    completion(.failure(description: "Server Error"))
                    return
                }
                completion(.failure(description: error.message))
            })
        case .networkError(let error):
            completion(.failure(description: error.localizedDescription))
        }
    }
    
    // MARK: - ZPScreenHookAdapterProtocol
    
    public func executeHook(presentationIndex: NSInteger,
                            dataDict: [String: Any]?,
                            taskFinishedWithCompletion: @escaping (Bool, NSError?, [String: Any]?) -> Void) {
        guard let controller = UIViewController.topmostViewController() else {
            assert(false, "No topmost controller")
            taskFinishedWithCompletion(false, nil, nil)
            return
        }
        
        let camFlowResult: (Bool) -> Void = { isFlowSucceded in
            taskFinishedWithCompletion(isFlowSucceded, nil, nil)
        }
        
        let cam = ContentAccessManager(rootViewController: controller,
                                       camDelegate: self,
                                       camFlow: flow,
                                       completion: camFlowResult)
        cam.startFlow()
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
        let api = CleengAPI.loginWithFacebook(publisherID: publisherId, email: userData.email,
                                              facebookId: userData.userId)
        authorize(api: api, completion: completion)
    }
    
    public func facebookSignUp(userData: (email: String, userId: String), completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.registerWithFacebook(publisherID: publisherId, email: userData.email,
                                                 facebookId: userData.userId)
        authorize(api: api, completion: completion)
    }
    
    public func login(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.login(publisherID: publisherId, email: authData["email"] ?? "",
                                  password: authData["password"] ?? "")
        authorize(api: api, completion: completion)
    }
    
    public func signUp(authData: [String: String], completion: @escaping (CAMResult) -> Void) {
        let api = CleengAPI.register(publisherID: publisherId, email: authData["email"] ?? "",
                                     password: authData["password"] ?? "")
        authorize(api: api, completion: completion)
    }
    
    public func resetPassword(data: [String: String], completion: @escaping (CAMResult) -> Void) {
        networkAdapter.resetPassword(data: data, completion: { (result) in
            switch result {
            case .success:
                completion(.success)
            case .failure(let error):
                self.camErrorWrapper(error: error, completion: completion)
            }
        })
    }
    
    public func itemPurchased(item: SKProduct) {
        
    }
    
    public func itemsRestored(items: [SKPaymentTransaction]) {
        
    }
    
    public func availableProducts() -> [Product] {
        return [Product]()
    }
}
