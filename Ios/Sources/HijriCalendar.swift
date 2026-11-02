import Foundation

protocol HijriCalendarProvider {
    var id: String { get }
    var displayName: String { get }
    var statusNote: String { get }
    var dateStatus: DateStatus { get }
    func hijri(from date: Date, adjustment: Int) -> DateComponents
    func gregorian(hijriYear: Int, month: Int, day: Int, adjustment: Int) -> Date?
}

/// Wraps Apple's built-in Islamic calendars. Add other conventions (e.g. moon-sighting feeds) as new providers.
struct AppleIslamicProvider: HijriCalendarProvider {
    let id: String
    let displayName: String
    let identifier: Calendar.Identifier
    var dateStatus: DateStatus { .calculated }
    var statusNote: String { "Calculated using \(displayName). Local moon-sighting announcements may differ." }

    private var islamic: Calendar { var c = Calendar(identifier: identifier); c.timeZone = .current; return c }
    private var greg: Calendar { var c = Calendar(identifier: .gregorian); c.timeZone = .current; return c }

    func hijri(from date: Date, adjustment: Int) -> DateComponents {
        let shifted = greg.date(byAdding: .day, value: adjustment, to: date) ?? date
        return islamic.dateComponents([.year, .month, .day], from: shifted)
    }

    func gregorian(hijriYear: Int, month: Int, day: Int, adjustment: Int) -> Date? {
        var c = DateComponents(); c.calendar = islamic; c.year = hijriYear; c.month = month; c.day = day; c.hour = 12
        guard let d = islamic.date(from: c) else { return nil }
        return greg.date(byAdding: .day, value: -adjustment, to: d)
    }

    static let all: [AppleIslamicProvider] = [
        .init(id: "umm_al_qura", displayName: "Umm al-Qura", identifier: .islamicUmmAlQura),
        .init(id: "civil", displayName: "Civil (tabular)", identifier: .islamicCivil),
        .init(id: "tabular", displayName: "Astronomical tabular", identifier: .islamicTabular)
    ]
    static func with(id: String) -> AppleIslamicProvider { all.first { $0.id == id } ?? all[0] }
}

enum HijriFormat {
    static let months = ["Muharram","Safar","Rabi' al-awwal","Rabi' al-thani","Jumada al-awwal","Jumada al-thani",
                         "Rajab","Sha'ban","Ramadan","Shawwal","Dhu al-Qi'dah","Dhu al-Hijjah"]
    static func string(_ c: DateComponents) -> String {
        guard let d = c.day, let m = c.month, let y = c.year, (1...12).contains(m) else { return "—" }
        return "\(d) \(months[m - 1]) \(y) AH"
    }
}

struct EventService {
    let provider: HijriCalendarProvider
    let adjustment: Int
    let events: [IslamicEvent]

    func events(on date: Date) -> [IslamicEvent] {
        let h = provider.hijri(from: date, adjustment: adjustment)
        return events.filter { $0.hijriDateRule.month == h.month && $0.hijriDateRule.day == h.day }
    }

    func occurrences(hijriYear: Int) -> [EventOccurrence] {
        events.compactMap { e in
            provider.gregorian(hijriYear: hijriYear, month: e.hijriDateRule.month, day: e.hijriDateRule.day, adjustment: adjustment)
                .map { EventOccurrence(event: e, hijriYear: hijriYear, gregorianDate: $0,
                                       status: e.datePolicy == "calendar_dependent" ? .estimated : provider.dateStatus) }
        }
    }

    func upcoming(from date: Date, limit: Int = 3) -> [EventOccurrence] {
        let y = provider.hijri(from: date, adjustment: adjustment).year ?? 1447
        let start = Calendar.current.startOfDay(for: date)
        return (occurrences(hijriYear: y) + occurrences(hijriYear: y + 1))
            .filter { $0.gregorianDate >= start }
            .sorted { $0.gregorianDate < $1.gregorianDate }
            .prefix(limit).map { $0 }
    }
}
