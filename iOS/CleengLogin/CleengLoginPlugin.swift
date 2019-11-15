//
//  CleengLoginPlugin.swift
//  CleengLogin
//
//  Created by Egor Brel on 6/3/19.
//

import ZappPlugins
import ZappLoginPluginsSDK
import CAM
import ComponentsSDK

let kCleengUserLoginToken = "CleengUserLoginToken"

typealias StoreID = String
typealias OfferID = String

@objc public class CleengLoginPlugin: NSObject, ZPLoginProviderUserDataProtocol, ZPAppLoadingHookProtocol, ZPScreenHookAdapterProtocol, ZPPluggableScreenProtocol {
    
    public var screenPluginDelegate: ZPPlugableScreenDelegate?
    private var accessChecker = AccessChecker()
    static var userToken: String?
    private var currentAvailableOfferIDs = [StoreID: OfferID]() // offerStoreID: OfferID
    private var offers: [CleengOffer] = []
    private var flowTrigger: Trigger = .appLaunch
    
    private var publisherId: String {
        return networkAdapter.publisherID
    }
    private var networkAdapter: CleengNetworkHandler!
    public var configurationJSON: NSDictionary?
    
    private var flow: CAMFlow = .no
    private var currentPlaybleItem: ZPPlayable?
    
    private var pluginConfiguration: [String: Any] = [:]
    private var camConfiguration: [String: String] = [:]
    
    public var isFlowBlocker: Bool {
        return true
    }
    
    public required override init() {
        super.init()
        
        assert(false, "Unexpected call of initialiizer")
    }
    
    public required init(configurationJSON: NSDictionary?) {
        super.init()
        
        self.configurationJSON = configurationJSON
        if var pluginConfiguration = ZLComponentsManager.screenComponentForPluginID("Cleeng")?.general {
            self.pluginConfiguration = pluginConfiguration
            for (key, value) in pluginConfiguration {
                switch value {
                case let string as String:
                    camConfiguration[key] = string
                case let bool as Bool:
                    camConfiguration[key] = bool.description
                default:
                    break
                }
            }
            
            if let publisherID = pluginConfiguration["cleeng_login_publisher_id"] as? String {
                networkAdapter = CleengNetworkHandler(publisherID: publisherID)
                networkAdapter.errorMessage = errorMessage()
            }
            
            if let authFieldsURLString = pluginConfiguration[CAMKeys.authFields.rawValue] as? String,
                let authFieldsURL = URL(string: authFieldsURLString),
                let authFieldsData = try? Data(contentsOf: authFieldsURL),
                let authFieldsStringData = String(data: authFieldsData, encoding: .utf8) {
                camConfiguration[CAMKeys.authFields.rawValue] = authFieldsStringData
            }
        }
    }
    
    public required init?(pluginModel: ZPPluginModel, screenModel: ZLScreenModel, dataSourceModel: NSObject?) {
        super.init()
        self.pluginConfiguration = screenModel.general
        for (key, value) in pluginConfiguration {
            switch value {
            case let string as String:
                camConfiguration[key] = string
            case let bool as Bool:
                camConfiguration[key] = bool.description
            default:
                break
            }
        }
        
        if let publisherID = pluginConfiguration["cleeng_login_publisher_id"] as? String {
            networkAdapter = CleengNetworkHandler(publisherID: publisherID)
            networkAdapter.errorMessage = errorMessage()
        }
        
        if let authFieldsURLString = pluginConfiguration[CAMKeys.authFields.rawValue] as? String,
            let authFieldsURL = URL(string: authFieldsURLString),
            let authFieldsData = try? Data(contentsOf: authFieldsURL),
            let authFieldsStringData = String(data: authFieldsData, encoding: .utf8) {
            camConfiguration[CAMKeys.authFields.rawValue] = authFieldsStringData
        }
        
        let playableItems = dataSourceModel as? [ZPPlayable] ?? []
        self.currentPlaybleItem = playableItems.first
        flow = accessChecker.getCamFlow(for: playableItems.first?.extensionsDictionary as? [String: Any])
    }
    
    public required init?(pluginModel: ZPPluginModel, dataSourceModel: NSObject?) {
        super.init()
        
        assert(false, "Unexpected call of initialiizer")
    }
    
    // MARK: - Private methods
    
    private func silentAuthorization(completion: @escaping () -> Void) {
        guard let savedLoginToken = UserDefaults.standard.string(forKey: kCleengUserLoginToken) else {
            completion()
            return
        }
        
        networkAdapter.extendToken(token: savedLoginToken) { _ in
            completion()
        }
    }
    
    private func authorize(api: CleengAPI, completion: @escaping (CAM.Result<Void>) -> Void) {
        networkAdapter.authorize(apiRequest: api) { (result) in
            switch result {
            case .success:
                completion(.success)
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }

    private func errorMessage() -> (ErrorCodes) -> String {
        let nonexistentAlertKey = "nonexistent_user_alert_text"
        let existingUserAlertKey = "existing_user_alert_text"
        let invalidCredentialsAlertKey = "invalid_credentials_alert_text"
        let defaultAlertKey = "default_alert_text"
        
        let nonexistentAlertMessage = pluginConfiguration[nonexistentAlertKey] as? String ?? ""
        let existingUserAlertMessage = pluginConfiguration[existingUserAlertKey] as? String ?? ""
        let invalidCredentialsAlertMessage = pluginConfiguration[invalidCredentialsAlertKey] as? String ?? ""
        let defaultAlertMessage = pluginConfiguration[defaultAlertKey] as? String ?? ""
        
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
        silentAuthorization(completion: { () in
            self.executeAfterAppRootPresentationFlow(displayViewController: displayViewController,
                                                     completion: completion)
        })
    }
    
    private func executeAfterAppRootPresentationFlow(displayViewController: UIViewController?,
                                                     completion: (() -> Swift.Void)?) {
        #if DEBUG
        let alert = UIAlertController(title: "", message: "Logout?", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Ok", style: .default) { _ in
            CleengLoginPlugin.userToken = nil
            UserDefaults.standard.removeObject(forKey: kCleengUserLoginToken)
            AccessChecker.userPermissionEntitlementsIds.removeAll()
            APAuthorizationManager.sharedInstance()?.updateAuthorizationTokens(withAuthorizationProviders: [])
            self.executeTriggerOnAppLaunchFlow(displayViewController: displayViewController, completion: completion)
        })
        alert.addAction(UIAlertAction(title: "Cancel", style: .default) { _ in
            self.executeTriggerOnAppLaunchFlow(displayViewController: displayViewController, completion: completion)
        })
        UIViewController.topmostViewController()?.present(alert, animated: true, completion: nil)
        #else
        executeTriggerOnAppLaunchFlow(displayViewController: displayViewController, completion: completion)
        #endif
    }
    
    private func executeTriggerOnAppLaunchFlow(displayViewController: UIViewController?, completion: (() -> Swift.Void)?) {
        flowTrigger = .appLaunch
        let flow = accessChecker.getStartupFlow(for: pluginConfiguration)
        if flow != .no {
            guard let controller = displayViewController else {
                completion?()
                return
            }
            let contentAccessManager = ContentAccessManager(rootViewController: controller,
                                                            camDelegate: self,
                                                            camFlow: flow,
                                                            completion: { _ in completion?() })
            contentAccessManager.startFlow()
        } else {
            completion?()
        }
    }
    
    // MARK: - ZPLoginProviderUserDataProtocol
 
    public func isUserComply(policies: [String: NSObject]) -> Bool {
        let result = accessChecker.isUserComply(policies: policies, isAuthenticated: isAuthenticated())
        return result
    }
    
    public func isUserComply(policies: [String: NSObject], completion: @escaping (Bool) -> Void) {
        let result = accessChecker.isUserComply(policies: policies, isAuthenticated: isAuthenticated())
        completion(result)
    }
    
    // MARK: - ZPLoginProviderProtocol
    
    public func login(_ additionalParameters: [String: Any]?,
                      completion: @escaping ((ZPLoginOperationStatus) -> Void)) {
    
        guard let controller = UIViewController.topmostViewController() else {
            assert(false, "No topmost controller")
            completion(.failed)
            return
        }
        
        var flow = self.flow
        
        if additionalParameters != nil {
            flow = accessChecker.getCamFlow(for: additionalParameters)
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
        return CleengLoginPlugin.userToken != nil
    }
    
    public func isPerformingAuthorizationFlow() -> Bool {
        return networkAdapter.isPerformingAuthorizationFlow

    }
    
    public func getUserToken() -> String {
        return CleengLoginPlugin.userToken ?? ""
    }
    
    // MARK: - JSON Response parsing
    
    private func parseCleengOffersResponse(json: Data) -> [CleengOffer] {
        guard let cleengOffers = try? JSONDecoder().decode([CleengOffer].self, from: json) else {
            return []
        }
        return cleengOffers
    }

    // MARK: - ZPScreenHookAdapterProtocol
    
    public func requestScreenPluginPresentation(completion: @escaping (Bool) -> Void) {
        completion(false)
    }
    
    public func executeHook(presentationIndex: NSInteger,
                            dataDict: [String: Any]?,
                            taskFinishedWithCompletion: @escaping (Bool, NSError?, [String: Any]?) -> Void) {
        flowTrigger = .tapCell
        login(nil) { (operationStatus) in
            switch operationStatus {
            case .completedSuccessfully:
                taskFinishedWithCompletion(true, nil, nil)
            case .failed, .cancelled:
                taskFinishedWithCompletion(false, nil, nil)
            }
        }
    }
    
    //MARK: ZPPluggableScreenProtocol
    
    public func createScreen() -> UIViewController {
        return UIViewController()
    }
}

// MARK: - CAMDelegate

extension CleengLoginPlugin: CAMDelegate {    
    
    public func getPluginConfig() -> [String: String] {
        return camConfiguration
    }
    
    public func isPurchaseNeeded() -> Bool {
        return accessChecker.isPurchaseNeeded
    }
    
    public func IsUserLoggedIn() -> Bool {
        return isAuthenticated()
    }
    
    public func facebookLogin(userData: (email: String, userId: String),
                              completion: @escaping (LoginResult) -> Void) {
        let api = CleengAPI.loginWithFacebook(publisherID: publisherId,
                                              email: userData.email,
                                              facebookId: userData.userId)
        authorize(api: api, completion: completion)
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
                completion(.success)
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    public func availableProducts(completion: @escaping (AvailableProductsResult) -> Void) {
        networkAdapter.subscriptions(token: CleengLoginPlugin.userToken, byAuthId: 1,
                                     offers: accessChecker.currentItemEntitlementsIds, completion: { (result) in
            switch result {
            case .success(let data):
                let offers = self.parseCleengOffersResponse(json: data)
                self.offers = offers
                self.currentAvailableOfferIDs = offers.reduce([:]) { (dict, item) -> [String: String] in
                    var dict = dict
                    dict[item.appleProductID] = item.offerID
                    return dict
                }
                let storeIDs = Array(self.currentAvailableOfferIDs.keys)
                completion(.success(storeIDs))
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    public func itemPurchased(purchasedItem: PurchasedProduct, completion: @escaping (PurchaseResult) -> Void) {
        guard let offerId = currentAvailableOfferIDs[purchasedItem.productIdentifier],
            let transactionId = purchasedItem.transaction.transactionIdentifier else {
                return
        }
        
        networkAdapter.purchaseItem(token: CleengLoginPlugin.userToken,
                                    offerId: offerId,
                                    transactionId: transactionId,
                                    receiptData: purchasedItem.receipt,
                                    isRestored: false) { result in
                                        switch result {
                                        case .success:
                                            completion(.success)
                                        case .failure(let error):
                                            completion(.failure(error))
                                        }
        }
    }
    
    public func itemsRestored(restoredItems: [PurchasedProduct],
                              completion: @escaping (PurchaseResult) -> Void) {
        let purchases = restoredItems.compactMap { (product) -> RestorePurchaseData? in
            guard let transactionId = product.transaction.transactionIdentifier else {
                return nil
            }
            return RestorePurchaseData(transactionId: transactionId,
                                       productId: product.productIdentifier)
        }
        
        let receipt = restoredItems.first!.receipt.base64EncodedString()
        networkAdapter.restore(purchases: purchases,
                               token: CleengLoginPlugin.userToken ?? "",
                               receipt: receipt) { (result) in
            switch result {
            case .success:
                completion(.success)
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    public func itemName() -> String {
        if flowTrigger == .appLaunch {
            return Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String ?? ""
        }
        
        return currentPlaybleItem?.playableName() ?? ""
    }
    
    public func itemType() -> String {
        if flowTrigger == .appLaunch {
            return "App"
        }
        
        guard let item = currentPlaybleItem else {
            return ""
        }
        
        if let isPlaylist = item.isPlaylist, isPlaylist == true {
            return "Feed"
        }
        
        return "Video"
    }
    
    public func purchaseProperties(for productIdentifier: String) -> PurchaseProperties {
        var purchaseProperties = PurchaseProperties(productIdentifier: productIdentifier,
                                                    isSubscriber: (AccessChecker.userPermissionEntitlementsIds.isEmpty == false))
        
        guard let offer = self.offers.first(where: { $0.appleProductID == productIdentifier }) else {
            return purchaseProperties
        }
        
        purchaseProperties.trialPeriod = offer.freeDays
        
        if let period = offer.period, period.isEmpty == false {
            purchaseProperties.subscriptionDuration = period
        }
        
        if let tags = offer.accessToTags, tags.isEmpty == false {
            purchaseProperties.purchaseEntityType = .category
        } else {
            purchaseProperties.purchaseEntityType = .vod
        }
        
        return purchaseProperties
    }
    
    public func trigger() -> Trigger {
        return flowTrigger
    }
}
