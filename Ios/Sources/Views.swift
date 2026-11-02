import SwiftUI

@main
struct IslamicCalendarApp: App {
    @StateObject private var settings = SettingsStore()
    @StateObject private var content = ContentRepository()
    @StateObject private var saved = SavedStore()
    var body: some Scene {
        WindowGroup {
            TabView {
                HomeView().tabItem { Label("Home", systemImage: "moon.stars") }
                CalendarTab().tabItem { Label("Calendar", systemImage: "calendar") }
                WisdomTab().tabItem { Label("Wisdom", systemImage: "book") }
                SettingsView().tabItem { Label("Settings", systemImage: "gearshape") }
            }
            .tint(Theme.primary)
            .environmentObject(settings).environmentObject(content).environmentObject(saved)
            .preferredColorScheme(settings.colorScheme)
            .environment(\.layoutDirection, settings.languageCode == "ar" ? .rightToLeft : .leftToRight)
            .fullScreenCover(isPresented: Binding(get: { !settings.onboarded }, set: { _ in })) {
                WelcomeView().environmentObject(settings).environmentObject(content)
            }
        }
    }
}

struct Card<C: View>: View {
    var hero = false
    @ViewBuilder let content: C
    var body: some View {
        VStack(alignment: .leading, spacing: 6) { content }
            .frame(maxWidth: .infinity, alignment: .leading).padding(18)
            .foregroundStyle(hero ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
            .background {
                if hero { RoundedRectangle(cornerRadius: 24).fill(Theme.hero) }
                else { RoundedRectangle(cornerRadius: 24).fill(.regularMaterial)
                    .overlay(RoundedRectangle(cornerRadius: 24).strokeBorder(.white.opacity(0.18))) }
            }
            .shadow(color: .black.opacity(0.10), radius: 14, y: 6)
    }
}

func eventService(_ s: SettingsStore, _ c: ContentRepository) -> EventService {
    EventService(provider: s.provider, adjustment: s.adjustment, events: c.events)
}

struct StatusBadge: View {
    let status: DateStatus
    var body: some View { Text(status.rawValue.capitalized).font(.caption2.bold()).padding(.horizontal, 8).padding(.vertical, 3)
        .background(.quaternary, in: Capsule()) }
}

// MARK: Home
struct HomeView: View {
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    var body: some View {
        let today = Date()
        let h = s.provider.hijri(from: today, adjustment: s.adjustment)
        let upcoming = eventService(s, c).upcoming(from: today, limit: 3)
        NavigationStack {
            ScrollView { VStack(spacing: 14) {
                Card(hero: true) {
                    Text(today.formatted(date: .complete, time: .omitted)).font(.headline)
                    Text(HijriFormat.string(h)).font(.system(.title, design: .rounded).bold())
                    Text(s.provider.statusNote).font(.footnote).foregroundStyle(.secondary)
                }
                if let l = c.featured(on: today, lang: s.languageCode) {
                    NavigationLink { LessonDetail(lesson: l) } label: {
                        Card {
                            Text("Today's wisdom").font(.caption).foregroundStyle(.secondary)
                            Label(l.takeaway, systemImage: "sparkles").font(.system(.title3, design: .rounded)).labelStyle(.titleOnly)
                            Text("Practice today: \(l.practicalAction)").font(.subheadline).foregroundStyle(.secondary)
                            Text("Read lesson →").font(.subheadline.bold())
                        }
                    }.buttonStyle(.plain)
                }
                Card {
                    Text("Upcoming").font(.caption).foregroundStyle(.secondary)
                    ForEach(upcoming) { o in
                        NavigationLink { EventDetail(event: o.event) } label: {
                            HStack { Text(o.event.name(s.languageCode)); Spacer()
                                Text(o.gregorianDate.formatted(date: .abbreviated, time: .omitted)).foregroundStyle(.secondary)
                                StatusBadge(status: o.status) }
                        }.buttonStyle(.plain)
                    }
                }
            }.padding() }
            .navigationTitle("Today").background(AppBackground())
        }
    }
}

// MARK: Calendar
struct DayID: Identifiable { let date: Date; var id: Date { date } }

let eventCategories = ["ramadan", "eid", "hajj", "islamic_new_year", "observance"]
func catLabel(_ k: String) -> String { k.replacingOccurrences(of: "_", with: " ").capitalized }

struct CalendarTab: View {
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    @State private var anchor = Date()
    @State private var hijriMode = false
    @State private var category: String?
    @State private var selected: DayID?
    private var cal: Calendar { var k = Calendar(identifier: .gregorian); k.firstWeekday = s.weekStartsSunday ? 1 : 2; return k }

    private var monthFirst: Date {
        if hijriMode {
            let h = s.provider.hijri(from: anchor, adjustment: s.adjustment)
            return s.provider.gregorian(hijriYear: h.year ?? 1447, month: h.month ?? 1, day: 1, adjustment: s.adjustment) ?? anchor
        }
        return cal.date(from: cal.dateComponents([.year, .month], from: anchor))!
    }
    private var monthDays: [Date] {
        let first = monthFirst
        if hijriMode {
            let m = s.provider.hijri(from: first, adjustment: s.adjustment).month
            return (0..<30).compactMap { cal.date(byAdding: .day, value: $0, to: first) }
                .filter { s.provider.hijri(from: $0, adjustment: s.adjustment).month == m }
        }
        let n = cal.range(of: .day, in: .month, for: first)!.count
        return (0..<n).compactMap { cal.date(byAdding: .day, value: $0, to: first) }
    }
    private var cells: [Date?] {
        let lead = (cal.component(.weekday, from: monthFirst) - cal.firstWeekday + 7) % 7
        return Array(repeating: nil, count: lead) + monthDays.map { Optional($0) }
    }
    private var title: String {
        if hijriMode {
            let h = s.provider.hijri(from: monthFirst, adjustment: s.adjustment)
            return "\(HijriFormat.months[(h.month ?? 1) - 1]) \(h.year ?? 0) AH"
        }
        return anchor.formatted(.dateTime.month(.wide).year())
    }
    private func step(_ dir: Int) {
        if hijriMode { anchor = cal.date(byAdding: .day, value: dir > 0 ? 30 : -1, to: monthFirst)! }
        else { anchor = cal.date(byAdding: .month, value: dir, to: anchor)! }
    }
    private func hasEvent(_ d: Date, _ svc: EventService) -> Bool {
        svc.events(on: d).contains { category == nil || $0.category == category }
    }

    var body: some View {
        let svc = eventService(s, c)
        let sym = cal.veryShortStandaloneWeekdaySymbols
        let heads = Array(sym[(cal.firstWeekday - 1)...] + sym[..<(cal.firstWeekday - 1)])
        NavigationStack {
            VStack {
                Picker("Calendar", selection: $hijriMode) { Text("Gregorian").tag(false); Text("Hijri").tag(true) }
                    .pickerStyle(.segmented).padding(.horizontal)
                HStack {
                    Button { step(-1) } label: { Image(systemName: "chevron.left") }
                    Spacer(); Text(title).font(.headline); Spacer()
                    Button("Today") { anchor = Date() }
                    Button { step(1) } label: { Image(systemName: "chevron.right") }
                }.padding(.horizontal)
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 7)) {
                    ForEach(Array(heads.enumerated()), id: \.offset) { Text($1).font(.caption).foregroundStyle(.secondary) }
                    ForEach(Array(cells.enumerated()), id: \.offset) { _, d in
                        if let d {
                            let g = cal.component(.day, from: d)
                            let h = s.provider.hijri(from: d, adjustment: s.adjustment).day ?? 0
                            Button { selected = DayID(date: d) } label: {
                                VStack(spacing: 1) {
                                    Text("\(hijriMode ? h : g)")
                                    Text("\(hijriMode ? g : h)").font(.caption2).foregroundStyle(.secondary)
                                    Circle().frame(width: 5, height: 5).opacity(hasEvent(d, svc) ? 1 : 0)
                                }.frame(maxWidth: .infinity, minHeight: 44)
                                .background(cal.isDateInToday(d) ? Theme.primary.opacity(0.28) : .clear, in: RoundedRectangle(cornerRadius: 8))
                            }.buttonStyle(.plain)
                        } else { Color.clear.frame(height: 44) }
                    }
                }.padding(.horizontal)
                Spacer()
            }
            .navigationTitle("Calendar").background(AppBackground())
            .toolbar {
                ToolbarItemGroup {
                    Menu { Picker("Category", selection: $category) {
                        Text("All occasions").tag(String?.none)
                        ForEach(eventCategories, id: \.self) { Text(catLabel($0)).tag(String?.some($0)) } }
                    } label: { Image(systemName: category == nil ? "line.3.horizontal.decrease.circle" : "line.3.horizontal.decrease.circle.fill") }
                    NavigationLink("Events") { EventsList() }
                }
            }
            .sheet(item: $selected) { DayDetail(date: $0.date).presentationDetents([.medium, .large]) }
        }
    }
}

struct DayDetail: View {
    let date: Date
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text(date.formatted(date: .complete, time: .omitted))
                    Text(HijriFormat.string(s.provider.hijri(from: date, adjustment: s.adjustment))).bold()
                    Text(s.provider.statusNote).font(.footnote).foregroundStyle(.secondary)
                }
                Section("Occasions") {
                    let ev = eventService(s, c).events(on: date)
                    if ev.isEmpty { Text("No occasions on this date").foregroundStyle(.secondary) }
                    ForEach(ev) { e in NavigationLink(e.name(s.languageCode)) { EventDetail(event: e) } }
                }
            }.navigationTitle("Day")
        }
    }
}

struct EventsList: View {
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    @State private var category: String?
    var body: some View {
        let occ = eventService(s, c).upcoming(from: Date(), limit: 100).filter { category == nil || $0.event.category == category }
        List(occ) { o in
            NavigationLink { EventDetail(event: o.event) } label: {
                VStack(alignment: .leading) { Text(o.event.name(s.languageCode))
                    Text(o.gregorianDate.formatted(date: .long, time: .omitted)).font(.footnote).foregroundStyle(.secondary) }
            }
        }
        .navigationTitle("Occasions")
        .toolbar { Menu { Picker("Category", selection: $category) {
            Text("All").tag(String?.none)
            ForEach(eventCategories, id: \.self) { Text(catLabel($0)).tag(String?.some($0)) } }
        } label: { Image(systemName: "line.3.horizontal.decrease.circle") } }
    }
}

struct EventDetail: View {
    let event: IslamicEvent
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    @EnvironmentObject var saved: SavedStore
    var body: some View {
        let occ = eventService(s, c).upcoming(from: Date(), limit: 100).first { $0.event.id == event.id }
        List {
            Section {
                Text(event.name(s.languageCode)).font(.title2.bold())
                Text(HijriFormat.months[event.hijriDateRule.month - 1] + " \(event.hijriDateRule.day)")
                if let occ {
                    HStack { Text(occ.gregorianDate.formatted(date: .long, time: .omitted)); Spacer(); StatusBadge(status: occ.status) }
                    Text("Estimated date. It may differ by country or local moon sighting.").font(.footnote).foregroundStyle(.secondary)
                }
            }
            Section { Text(event.text(s.languageCode)) }
            if event.regionScope != "global" || !event.traditionTags.isEmpty {
                Section("Note") { Text("Observed differently across communities (\(event.traditionTags.joined(separator: ", "))).") }
            }
            Section {
                Toggle("Remind me", isOn: Binding(
                    get: { saved.reminderEvents.contains(event.id) },
                    set: { on in
                        guard let occ else { return }
                        if on { Notifier.requestIfNeeded { ok in
                            guard ok else { return }
                            saved.setEventReminder(event.id, on: true)
                            Notifier.scheduleEvent(id: event.id, title: event.name(s.languageCode), on: occ.gregorianDate) } }
                        else { saved.setEventReminder(event.id, on: false); Notifier.cancel(event.id) }
                    }))
                Button(saved.isSaved("event", event.id) ? "Remove from saved" : "Save") { saved.toggle("event", event.id) }
                ShareLink(item: "\(event.name(s.languageCode)) — \(occ?.gregorianDate.formatted(date: .long, time: .omitted) ?? "")")
            }
        }.navigationTitle("Event")
    }
}

// MARK: Wisdom
struct WisdomTab: View {
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    @EnvironmentObject var saved: SavedStore
    @State private var query = ""
    @State private var category: String?
    @State private var type: ContentType?
    @State private var maxMinutes: Int?
    var body: some View {
        let all = c.lessons(in: s.languageCode)
        let cats = Array(Set(all.map(\.categoryId))).sorted()
        let shown = all.filter { l in
            let okCat = category == nil || l.categoryId == category
            let okType = type == nil || l.contentType == type
            let okTime = maxMinutes == nil || l.readingTimeMinutes <= (maxMinutes ?? 99)
            let okQuery = query.isEmpty || [l.title, l.takeaway, l.body].contains { $0.localizedCaseInsensitiveContains(query) }
            return okCat && okType && okTime && okQuery
        }
        let evs = query.isEmpty ? [] : c.events.filter { e in e.names.values.contains { $0.localizedCaseInsensitiveContains(query) } }
        NavigationStack {
            List {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack { Button("All") { category = nil }.buttonStyle(.bordered)
                        ForEach(cats, id: \.self) { k in Button(catLabel(k)) { category = k }.buttonStyle(.bordered) } }
                }.listRowSeparator(.hidden)
                if !evs.isEmpty { Section("Occasions") {
                    ForEach(evs) { e in NavigationLink(e.name(s.languageCode)) { EventDetail(event: e) } } } }
                Section("Lessons") {
                    ForEach(shown) { l in
                        NavigationLink { LessonDetail(lesson: l) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(l.title).bold(); Text(l.takeaway).font(.subheadline).foregroundStyle(.secondary)
                                Text("\(l.contentType.label) · \(l.readingTimeMinutes) min").font(.caption2).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                let savedLessons = saved.items.filter { $0.itemType == "lesson" }
                if !savedLessons.isEmpty { Section("Saved") {
                    ForEach(savedLessons) { it in
                        if let l = all.first(where: { $0.id == it.itemId }) { NavigationLink(l.title) { LessonDetail(lesson: l) } }
                    } } }
            }
            .searchable(text: $query).navigationTitle("Wisdom").scrollContentBackground(.hidden).background(AppBackground())
            .toolbar { Menu {
                Picker("Type", selection: $type) {
                    Text("Any type").tag(ContentType?.none)
                    ForEach(ContentType.allCases, id: \.self) { Text($0.label).tag(ContentType?.some($0)) } }
                Picker("Reading time", selection: $maxMinutes) {
                    Text("Any length").tag(Int?.none); Text("≤ 1 min").tag(Int?.some(1))
                    Text("≤ 2 min").tag(Int?.some(2)); Text("≤ 5 min").tag(Int?.some(5)) }
            } label: { Image(systemName: "line.3.horizontal.decrease.circle") } }
        }
    }
}

struct LessonDetail: View {
    let lesson: MoralLesson
    @EnvironmentObject var c: ContentRepository
    @EnvironmentObject var saved: SavedStore
    var body: some View {
        ScrollView { VStack(alignment: .leading, spacing: 14) {
            Text(lesson.contentType.label).font(.caption.bold()).foregroundStyle(.secondary)
            if lesson.translationStatus == "machine_translated" { Text("Machine-translated; awaiting human review.").font(.footnote).foregroundStyle(.orange) }
            Text(lesson.title).font(.title.bold())
            Text(lesson.takeaway).font(.title3)
            Text(lesson.body)
            Card { Text("Practice").font(.caption).foregroundStyle(.secondary); Text(lesson.practicalAction) }
            if let q = lesson.reflectionQuestion { Card { Text("Reflect").font(.caption).foregroundStyle(.secondary); Text(q) } }
            ForEach(c.references.filter { lesson.sourceIds.contains($0.id) }) { r in
                Card { Text(r.reference ?? "Reference pending").bold()
                    if r.verificationStatus != "verified" { Text("Source not yet verified").font(.footnote).foregroundStyle(.orange) }
                    if let t = r.translationText { Text(t) }
                    if let tr = r.translator { Text("Translation: \(tr)").font(.footnote) } }
            }
            HStack {
                Button(saved.isSaved("lesson", lesson.id) ? "Saved ✓" : "Save") { saved.toggle("lesson", lesson.id) }
                ShareLink(item: "\(lesson.title): \(lesson.takeaway)")
                Button(saved.read.contains(lesson.id) ? "Read ✓" : "Mark as read") { saved.markRead(lesson.id) }
            }.buttonStyle(.bordered)
        }.padding() }
    }
}

// MARK: Settings
struct SettingsView: View {
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    var body: some View {
        NavigationStack {
            Form {
                Section("Language & region") {
                    Picker("Language", selection: $s.languageCode) { Text("English").tag("en"); Text("العربية").tag("ar") }
                    TextField("Region code (e.g. IN)", text: $s.regionCode).textInputAutocapitalization(.characters)
                }
                Section(header: Text("Hijri calendar"), footer: Text("Calculated dates may differ from local moon-sighting announcements. Use the adjustment to match your community.")) {
                    Picker("Method", selection: $s.method) { ForEach(AppleIslamicProvider.all, id: \.id) { Text($0.displayName).tag($0.id) } }
                    Stepper("Adjustment: \(s.adjustment > 0 ? "+" : "")\(s.adjustment) day(s)", value: $s.adjustment, in: -2...2)
                    Toggle("Week starts on Sunday", isOn: $s.weekStartsSunday)
                }
                Section("Notifications") {
                    DailyReminderToggle()
                    if s.dailyReminder { DatePicker("Time", selection: $s.reminderTime, displayedComponents: .hourAndMinute)
                        .onChange(of: s.reminderTime) { _, t in Notifier.scheduleDaily(at: t, title: "Daily Wisdom", body: "Today's lesson is ready.") } }
                }
                Section("Appearance") {
                    Picker("Theme", selection: $s.appearance) { Text("System").tag("system"); Text("Light").tag("light"); Text("Dark").tag("dark") }
                }
                Section("About") {
                    NavigationLink("Sources, editorial policy & privacy") { SourcesPolicyView() }
                    Text("Offline content: \(c.events.count) occasions, \(c.lessons.count) lessons").foregroundStyle(.secondary)
                    Text("Original reflections and stories are labelled and never presented as Qur'an or Hadith. Saved items and preferences stay on this device. No accounts, ads, audio or analytics.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }.navigationTitle("Settings").scrollContentBackground(.hidden).background(AppBackground())
        }
    }
}

struct DailyReminderToggle: View {
    @EnvironmentObject var s: SettingsStore
    @EnvironmentObject var c: ContentRepository
    var body: some View {
        Toggle("Daily wisdom reminder", isOn: Binding(get: { s.dailyReminder }, set: { on in
                        if on { Notifier.requestIfNeeded { ok in
                            s.dailyReminder = ok
                            if ok, let l = c.featured(on: Date(), lang: s.languageCode) { Notifier.scheduleDaily(at: s.reminderTime, title: "Daily Wisdom", body: l.takeaway) } } }
                        else { s.dailyReminder = false; Notifier.cancel("daily") } }))
    }
}

// MARK: Welcome / setup
struct WelcomeView: View {
    @EnvironmentObject var s: SettingsStore
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(spacing: 8) {
                        Image(systemName: "moon.stars.fill").font(.system(size: 56))
                        Text("Islamic Calendar & Daily Wisdom").font(.title2.bold()).multilineTextAlignment(.center)
                        Text("Free · No ads · No audio · Works offline").font(.footnote).foregroundStyle(.secondary)
                    }.frame(maxWidth: .infinity)
                }
                Section("Language") { Picker("Language", selection: $s.languageCode) { Text("English").tag("en"); Text("العربية").tag("ar") } }
                Section(header: Text("Region"), footer: Text("Used for regional occasion notes. No location permission needed.")) {
                    TextField("Region code (e.g. IN)", text: $s.regionCode).textInputAutocapitalization(.characters) }
                Section(header: Text("Hijri calendar"), footer: Text("Choose the convention your community follows. You can shift dates by up to 2 days to match local moon sighting.")) {
                    Picker("Method", selection: $s.method) { ForEach(AppleIslamicProvider.all, id: \.id) { Text($0.displayName).tag($0.id) } }
                    Stepper("Adjustment: \(s.adjustment > 0 ? "+" : "")\(s.adjustment) day(s)", value: $s.adjustment, in: -2...2) }
                Section(footer: Text("Optional. You can change this any time in Settings.")) { DailyReminderToggle() }
                Section { Button("Start using the app") { s.onboarded = true }.frame(maxWidth: .infinity).bold() }
            }.navigationTitle("Welcome")
        }
    }
}

// MARK: Sources, editorial policy, privacy
struct SourcesPolicyView: View {
    @EnvironmentObject var c: ContentRepository
    static let contactEmail = "contact@your-domain.example"   // TODO: replace before release
    var body: some View {
        let verified = c.references.filter { $0.verificationStatus == "verified" }.count
        List {
            Section("Review status") {
                Text("Content is in draft. No lesson or occasion has yet been reviewed by a qualified religious editor. Reviewer names and credentials will be listed here once review is complete.")
                Text("\(verified) of \(c.references.count) religious references verified.").foregroundStyle(.secondary)
            }
            Section("Content types") {
                Text("Lessons are labelled as Qur'an passage, Hadith, moral reflection or illustrative story. Original reflections and stories are never presented as religious quotations or historical events.")
            }
            Section("Calendar conventions") {
                Text("Dates are calculated with the method you choose and are not announcements. Eid, Ramadan and other dates can differ by country or moon sighting; use the day adjustment to follow your local authority.")
            }
            Section("Translations") {
                Text("Machine-translated text is marked until a human reviewer approves it. Translation credits appear on each sourced lesson.")
            }
            Section("Privacy") {
                Text("No account, ads, analytics or audio. Preferences, saved items and reading history stay on this device. Notifications are local and optional.")
            }
            Section("Report an error") {
                Link("Email a correction", destination: URL(string: "mailto:\(Self.contactEmail)?subject=Content%20correction")!)
            }
            Section { Text("Version 1.0 (draft)").foregroundStyle(.secondary) }
        }.navigationTitle("Sources & policy")
    }
}
