import Foundation

enum DateStatus: String, Codable { case calculated, estimated, announced, confirmed, superseded }
enum ContentType: String, Codable, CaseIterable {
    case quran, hadith
    case moralReflection = "moral_reflection"
    case illustrativeStory = "illustrative_story"
    var label: String {
        switch self {
        case .quran: return "Qur'an passage"
        case .hadith: return "Hadith"
        case .moralReflection: return "Moral reflection (not a religious text)"
        case .illustrativeStory: return "Illustrative story (fictional)"
        }
    }
}

struct HijriDateRule: Codable, Hashable { let type: String; let month: Int; let day: Int }

struct IslamicEvent: Codable, Identifiable, Hashable {
    let id: String, slug: String
    let names: [String: String]
    let category: String
    let hijriDateRule: HijriDateRule
    let description: [String: String]
    let regionScope: String
    let traditionTags: [String]
    let datePolicy: String
    let sourceIds: [String]
    let contentStatus: String
    func name(_ lang: String) -> String { names[lang] ?? names["en"] ?? id }
    func text(_ lang: String) -> String { description[lang] ?? description["en"] ?? "" }
}

struct EventOccurrence: Identifiable, Hashable {
    var id: String { "\(event.id)_\(hijriYear)" }
    let event: IslamicEvent
    let hijriYear: Int
    let gregorianDate: Date
    let status: DateStatus
}

struct MoralLesson: Codable, Identifiable, Hashable {
    let id: String
    let contentType: ContentType
    let categoryId: String
    let languageCode: String
    let title, takeaway, body, practicalAction: String
    let reflectionQuestion: String?
    let readingTimeMinutes: Int
    let sourceIds: [String]
    let reviewStatus: String
    var translationStatus: String?
}

struct LessonTranslation: Codable, Hashable {
    let lessonId, languageCode, title, takeaway, body, practicalAction: String
    let reflectionQuestion: String?
    let translationStatus: String   // draft | machine_translated | human_reviewed | published
}
struct TranslationsFile: Codable { let schemaVersion: Int; let translations: [LessonTranslation] }

/// Placeholder-friendly: records stay "pending" until a human verifies them.
struct ReligiousReference: Codable, Identifiable, Hashable {
    let id: String
    let sourceType: String
    let reference: String?
    let originalText: String?
    let translationText: String?
    let translator: String?
    let verificationStatus: String
}

struct EventsFile: Codable { let schemaVersion: Int; let events: [IslamicEvent] }
struct LessonsFile: Codable { let schemaVersion: Int; let lessons: [MoralLesson]; let references: [ReligiousReference] }

struct SavedItem: Codable, Identifiable, Hashable {
    let id: String
    let itemType: String   // "lesson" | "event"
    let itemId: String
    let savedAt: Date
}
