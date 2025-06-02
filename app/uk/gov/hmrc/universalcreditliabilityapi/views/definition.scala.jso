@(appConfig: AppConfig)

{
    "api" : {
        "name": "Hello World",
        "description": "A 'hello world' example of an API on the HMRC API Developer Hub.",
        "context": "discuss-with-the-api-platform-team",
        "categories": [
            "OTHER"
        ],
        "versions": @versions
    }
}

@versions = {
    [
        {
            "version": "1.0",
            "status": "@appConfig.apiPlatformStatus.getOrElse("ALPHA")",
            "endpointsEnabled": @appConfig.apiPlatformEndpointsEnabled.getOrElse(false)
        }
    ]
}
