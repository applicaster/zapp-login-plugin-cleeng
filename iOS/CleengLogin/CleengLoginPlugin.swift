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

private let kCleengUserLoginToken = "CleengUserLoginToken"

typealias StoreID = String
typealias OfferID = String

@objc public class CleengLoginPlugin: NSObject, ZPLoginProviderUserDataProtocol, ZPAppLoadingHookProtocol, ZPScreenHookAdapterProtocol, ZPPluggableScreenProtocol {
    
    public var screenPluginDelegate: ZPPlugableScreenDelegate?
    private var accessChecker = AccessChecker()
    private static var userToken: String?
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
        
        if var pluginConfiguration = ZLComponentsManager.screenComponentForPluginID("cleeng_cam")?.general {
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
        
        let playableItems = dataSourceModel as? [ZPPlayable] ?? []
        self.currentPlaybleItem = playableItems.first
        flow = accessChecker.getCamFlow(for: playableItems.first?.extensionsDictionary as? [String: Any],
                                        isAuthenticated: isAuthenticated())
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
        networkAdapter.extendToken(token: savedLoginToken, completion: { (result) in
            switch result {
            case .success(let data):
                _ = self.parseAuthTokensResponse(json: data)
            case .failure:
                break
            }
            completion()
        })
    }
    
    private func authorize(api: CleengAPI, completion: @escaping (CAM.Result<Void>) -> Void) {
        networkAdapter.authorize(apiRequest: api, completion: { (result) in
            switch result {
            case .success(let data):
                let isParsed = self.parseAuthTokensResponse(json: data)
                if isParsed {
                    completion(.success)
                } else {
                    let errorCode = ErrorCodes.unknown
                    let errorMessage = self.pluginConfiguration["default_alert_text"] as? String ?? ""
                    let error = RequestError(from: errorCode,
                                             with: errorMessage)
                    completion(.failure(CleengError.authTokenNotParsed(error)))
                }
            case .failure(let error):
                completion(.failure(error))
            }
        })
    }
    
    private func purchaseItem(token: String,
                              offerId: String,
                              transactionId: String,
                              receiptData: Data,
                              isRestored: Bool, completion: @escaping (ItemPurchasingResult) -> Void) {
        networkAdapter.purchaseItem(token: CleengLoginPlugin.userToken, offerId: offerId,
                                    transactionId: transactionId, receiptData: receiptData,
                                    isRestored: isRestored) { (result) in
            switch result {
            case .success:
                self.verifyOnCleeng(offerId: offerId, completion: completion)
            case .failure(let error):
                completion(.failure(error))
                return
            }
        }
        
    }
    
    private func verifyOnCleeng(offerId: String, completion: @escaping (ItemPurchasingResult) -> Void) {
        let timerStartTime = Date()
        Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { timer in
            if Date().timeIntervalSince(timerStartTime) > 60 {
                timer.invalidate()
                let errorMessage = self.pluginConfiguration["default_alert_text"] as? String ?? ""
                let error = RequestError(from: ErrorCodes.unknown, with: errorMessage)
                completion(.failure(.serverDoesntVerifyPurchase(error)))
                return
            }
            guard let userToken = CleengLoginPlugin.userToken else {
                timer.invalidate()
                let errorCode = ErrorCodes.unknown
                let errorMessage = self.pluginConfiguration["default_alert_text"] as? String ?? ""
                let error = RequestError(from: errorCode,
                                         with: errorMessage)
                completion(.failure(.authTokenNotParsed(error)))
                return
            }
            self.networkAdapter.extendToken(token: userToken, completion: { (result) in
                switch result {
                case .success(let data):
                    let _ = self.parseAuthTokensResponse(json: data)
                    guard let cleengTokens = try? JSONDecoder().decode([CleengToken].self, from: data) else {
                        return
                    }
                    let isOfferVerified = cleengTokens.contains(where: { (item) -> Bool in
                        return item.offerID == offerId
                    })
                    if isOfferVerified {
                        timer.invalidate()
                        completion(.success)
                        return
                    }
                case .failure:
                    break
                }
            })
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
        flowTrigger = .appLaunch
        let flow = accessChecker.getStartupFlow(for: pluginConfiguration,
                                                isAuthenticated: isAuthenticated())
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
            flow = accessChecker.getCamFlow(for: additionalParameters, isAuthenticated: isAuthenticated())
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
    
    private func parseAuthTokensResponse(json: Data) -> Bool {
        guard let cleengTokens = try? JSONDecoder().decode([CleengToken].self, from: json) else {
            return false
        }
        for item in cleengTokens {
            if item.offerID.isEmpty {
                CleengLoginPlugin.userToken = item.token // if offerID empty than we retrieve user token
                UserDefaults.standard.set(item.token, forKey: kCleengUserLoginToken)
            } else {
                if let authID = item.authID {
                    AccessChecker.userPermissionEntitlementsIds.insert(authID) // if offerID !empty put
                                                                 //subscription token in dicrionary by authId
                    APAuthorizationManager.sharedInstance().setAuthorizationToken(item.token,
                                                                                  withAuthorizationProviderID: authID) //set auth token for auth id. Need for applicaster player.
                }
            }
        }
        return true
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
        
        self.purchaseItem(token: CleengLoginPlugin.userToken ?? "",
                          offerId: offerId,
                          transactionId: transactionId,
                          receiptData: purchasedItem.receipt,
                          isRestored: false) { (result) in
            switch result {
            case .success:
                completion(.success)
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
    
    public func itemsRestored(restoredItems: [PurchasedProduct], completion: @escaping (PurchaseResult) -> Void) {
        let restoredOffers = restoredItems.reduce([]) { (array, item) -> [(offerId: String, restoredItem: PurchasedProduct)] in
            var array = array
            
            guard let offerId = currentAvailableOfferIDs[item.productIdentifier] else {
                    return array
            }
            array.append((offerId: offerId, restoredItem: item))
            return array
        }
        let dispatchGroup = DispatchGroup()
        var hasAccess = false
        
        var restoreError: Error?
        
        restoredOffers.forEach { (item) in
            dispatchGroup.enter()
            self.purchaseItem(token: CleengLoginPlugin.userToken ?? "",
                              offerId: item.offerId,
                              transactionId: item.restoredItem.transaction.transactionIdentifier ?? "",
                              receiptData: item.restoredItem.receipt,
                              isRestored: true) { (result) in
                switch result {
                case .success:
                    hasAccess = true
                case .failure(let error):
                    restoreError = error
                }
                dispatchGroup.leave()
            }
        }
        dispatchGroup.notify(queue: DispatchQueue.main) {
            hasAccess == true ? completion(.success) : completion(.failure(restoreError!))
        }
    }
    
    public func itemName() -> String {
        return currentPlaybleItem?.playableName() ?? ""
    }
    
    public func itemType() -> String {
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
