import SwiftUI
import UIKit

extension Color {
    init(light: UInt32, dark: UInt32) {
        self.init(UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }
}
extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
    }
}

/// Calm indigo + teal palette with a soft gold accent: vivid enough to feel modern, gentle enough for daily reading.
enum Theme {
    static let primary = Color(light: 0x1FA89B, dark: 0x3CCFC0)
    static let indigo = Color(light: 0x5563E8, dark: 0x7C89FF)
    static let gold = Color(light: 0xC9972F, dark: 0xF2C777)
    static let hero = LinearGradient(colors: [Color(hex: 0x5563E8), Color(hex: 0x1FA89B)], startPoint: .topLeading, endPoint: .bottomTrailing)
}
extension Color { init(hex: UInt32) { self.init(UIColor(hex: hex)) } }

struct AppBackground: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(light: 0xEFF4FF, dark: 0x0E1726), Color(light: 0xF3FBF9, dark: 0x14233B)], startPoint: .top, endPoint: .bottom)
            Circle().fill(Theme.indigo.opacity(0.18)).frame(width: 320).blur(radius: 90).offset(x: -140, y: -280)
            Circle().fill(Theme.primary.opacity(0.16)).frame(width: 300).blur(radius: 90).offset(x: 150, y: 300)
        }.ignoresSafeArea()
    }
}
