Pod::Spec.new do |s|
  s.name         = 'CleengLogin'
  s.version      = '2.5.4'
  s.summary      = 'Cleeng login plugin'
  s.license      = 'MIT'
  s.homepage     = 'https://github.com/applicaster/zapp-login-plugin-cleeng'
  s.author       = {"Applicaster" => "developer@applicaster.com"}
  s.ios.deployment_target = '10.0'
  s.swift_version = '5.1'
  s.source       = { :git => "https://github.com/applicaster/zapp-login-plugin-cleeng", :tag => 'ios-' + s.version.to_s }
  s.source_files = 'iOS/CleengLogin/**/*.{swift,h,m}'
  s.requires_arc = true
  s.static_framework = true

  s.dependency 'ZappPlugins'
  s.dependency 'Alamofire'
  s.dependency 'CAM', '3.5.2'
  s.dependency 'ApplicasterSDK'

  s.xcconfig =  { 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES',
    'ENABLE_BITCODE' => 'YES',
    'SWIFT_VERSION' => '5.1'
  }
end
