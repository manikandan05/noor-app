import SwiftUI
import UserNotifications

final class SettingsStore: ObservableObject {
    private let d = UserDefaults.standard
    @Published var languageCode: String { didSet { d.set(languageCode, forKey: "lang") } }
    @Published var regionCode: String { didSet { d.set(regionCode, forKey: "region") } }
    @Published var method: String { didSet { d.set(method, forKey: "method") } }
    @Published var adjustment: Int { didSet { d.set(adjustment, forKey: "adj") } }
    @Published var weekStartsSunday: Bool { didSet { d.set(weekStartsSunday, forKey: "sun") } }
    @Published var appearance: String { didSet { d.set(appearance, forKey: "appearance") } }
    @Published var onboarded: Bool { didSet { d.set(onboarded, forKey: "onboarded") } }
    @Published var dailyReminder: Bool { didSet { d.set(dailyReminder, forKey: "dailyOn") } }
    @Published var reminderTime: Date { didSet { d.set(reminderTime, forKey: "dailyTime") } }

    init() {
        languageCode = d.string(forKey: "lang") ?? "en"
        regionCode = d.string(forKey: "region") ?? (Locale.current.region?.identifier ?? "US")
        method = d.string(forKey: "method") ?? "umm_al_qura"
        adjustment = d.object(forKey: "adj") as? Int ?? 0
        weekStartsSunday = d.object(forKey: "sun") as? Bool ?? true
        appearance = d.string(forKey: "appearance") ?? "system"
        onboarded = d.bool(forKey: "onboarded")
        dailyReminder = d.bool(forKey: "dailyOn")
        reminderTime = d.object(forKey: "dailyTime") as? Date ?? Calendar.current.date(bySettingHour: 8, minute: 0, second: 0, of: Date())!
    }
    var provider: AppleIslamicProvider { .with(id: method) }
    var colorScheme: ColorScheme? { appearance == "light" ? .light : appearance == "dark" ? .dark : nil }
}

final class ContentRepository: ObservableObject {
    @Published var events: [IslamicEvent] = []
    @Published var lessons: [MoralLesson] = []
    @Published var references: [ReligiousReference] = []
    @Published var translations: [LessonTranslation] = []
    init() {
        events = Self.load("events", EventsFile.self)?.events ?? []
        if let f = Self.load("lessons", LessonsFile.self) { lessons = f.lessons; references = f.references }
        translations = Self.load("translations", TranslationsFile.self)?.translations ?? []
    }
    static func load<T: Decodable>(_ name: String, _ t: T.Type) -> T? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }
    /// English is the base; other languages overlay translations by stable lesson ID, falling back to English.
    func lessons(in lang: String) -> [MoralLesson] {
        let base = lessons.filter { $0.languageCode == "en" }
        guard lang != "en" else { return base }
        return base.map { l in
            guard let t = translations.first(where: { $0.lessonId == l.id && $0.languageCode == lang }) else { return l }
            return MoralLesson(id: l.id, contentType: l.contentType, categoryId: l.categoryId, languageCode: lang,
                               title: t.title, takeaway: t.takeaway, body: t.body, practicalAction: t.practicalAction,
                               reflectionQuestion: t.reflectionQuestion, readingTimeMinutes: l.readingTimeMinutes,
                               sourceIds: l.sourceIds, reviewStatus: l.reviewStatus, translationStatus: t.translationStatus)
        }
    }
    /// Featured lesson chosen by date (not a rigid one-per-day mapping).
    func featured(on date: Date, lang: String) -> MoralLesson? {
        let l = lessons(in: lang); guard !l.isEmpty else { return nil }
        let day = Calendar.current.ordinality(of: .day, in: .era, for: date) ?? 0
        return l[day % l.count]
    }
}

final class SavedStore: ObservableObject {
    @Published private(set) var items: [SavedItem] = [] { didSet { save() } }
    @Published private(set) var read: Set<String> = [] { didSet { UserDefaults.standard.set(Array(read), forKey: "read") } }
    @Published private(set) var reminderEvents: Set<String> = [] { didSet { UserDefaults.standard.set(Array(reminderEvents), forKey: "remEvents") } }
    init() {
        if let d = UserDefaults.standard.data(forKey: "saved"),
           let v = try? JSONDecoder().decode([SavedItem].self, from: d) { items = v }
        read = Set(UserDefaults.standard.stringArray(forKey: "read") ?? [])
        reminderEvents = Set(UserDefaults.standard.stringArray(forKey: "remEvents") ?? [])
    }
    private func save() { UserDefaults.standard.set(try? JSONEncoder().encode(items), forKey: "saved") }
    func isSaved(_ type: String, _ id: String) -> Bool { items.contains { $0.itemType == type && $0.itemId == id } }
    func toggle(_ type: String, _ id: String) {
        if let i = items.firstIndex(where: { $0.itemType == type && $0.itemId == id }) { items.remove(at: i) }
        else { items.append(SavedItem(id: UUID().uuidString, itemType: type, itemId: id, savedAt: Date())) }
    }
    func markRead(_ id: String) { read.insert(id) }
    func setEventReminder(_ id: String, on: Bool) { if on { reminderEvents.insert(id) } else { reminderEvents.remove(id) } }
}

/// Local notifications only. Permission is requested lazily, when the user enables a reminder.
enum Notifier {
    static func requestIfNeeded(_ done: @escaping (Bool) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { ok, _ in
            DispatchQueue.main.async { done(ok) }
        }
    }
    static func scheduleDaily(at time: Date, title: String, body: String) {
        let c = UNUserNotificationCenter.current(); c.removePendingNotificationRequests(withIdentifiers: ["daily"])
        let content = UNMutableNotificationContent(); content.title = title; content.body = body
        let comps = Calendar.current.dateComponents([.hour, .minute], from: time)
        c.add(UNNotificationRequest(identifier: "daily", content: content,
              trigger: UNCalendarNotificationTrigger(dateMatching: comps, repeats: true)))
    }
    static func cancel(_ id: String) { UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id]) }
    static func scheduleEvent(id: String, title: String, on date: Date) {
        let content = UNMutableNotificationContent(); content.title = title
        content.body = "Date is calculated and may differ with local moon sighting."
        var comps = Calendar.current.dateComponents([.year, .month, .day], from: date); comps.hour = 9
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: id, content: content,
              trigger: UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)))
    }
}
