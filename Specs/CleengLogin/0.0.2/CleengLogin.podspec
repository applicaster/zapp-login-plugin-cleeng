Pod::Spec.new do |s|
  s.name         = 'CleengLogin'
  s.version      = '0.0.2'
  s.summary      = 'Cleeng login plugin'
  s.license      = 'MIT'
  s.homepage     = 'https://github.com/applicaster/zapp-login-plugin-cleeng'
  s.author       = {"Brel Egor" => "brel@scand.com"}
  s.ios.deployment_target = '10.0'
  s.swift_version = '4.2'
  s.source       = { :git => "git@github.com:applicaster/zapp-login-plugin-cleeng.git", :tag => 'ios-' + s.version.to_s }
  s.source_files = 'iOS/CleengLogin/**/*.{swift,h,m}'
  s.requires_arc = true
  s.static_framework = true
  s.dependency 'ZappPlugins'
  s.dependency 'ZappLoginPluginsSDK'  
  s.dependency 'Alamofire'
  s.dependency 'CAM', '= 1.0.1'
  s.xcconfig =  { 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES',
    'ENABLE_BITCODE' => 'YES',
    'SWIFT_VERSION' => '4.2'
  }
end
