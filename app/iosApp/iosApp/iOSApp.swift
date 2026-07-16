import SwiftUI
import shared

@main
struct iOSApp: App {

    init(){
        PlatformKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}