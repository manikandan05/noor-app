import XCTest
@testable import IslamicCalendarApp

final class CalendarTests: XCTestCase {
    func testRoundTrip() {
        let p = AppleIslamicProvider.with(id: "umm_al_qura")
        for adj in -2...2 {
            let g = p.gregorian(hijriYear: 1448, month: 10, day: 1, adjustment: adj)!
            let h = p.hijri(from: g, adjustment: adj)
            XCTAssertEqual(h.year, 1448); XCTAssertEqual(h.month, 10); XCTAssertEqual(h.day, 1)
        }
    }
    func testAdjustmentShiftsGregorianDate() {
        let p = AppleIslamicProvider.with(id: "civil")
        let a = p.gregorian(hijriYear: 1448, month: 9, day: 1, adjustment: 0)!
        let b = p.gregorian(hijriYear: 1448, month: 9, day: 1, adjustment: 1)!
        XCTAssertEqual(Calendar.current.dateComponents([.day], from: b, to: a).day, 1)
    }
    func testHijriMonthHasTwentyNineOrThirtyDays() {
        let p = AppleIslamicProvider.with(id: "umm_al_qura")
        let first = p.gregorian(hijriYear: 1448, month: 9, day: 1, adjustment: 0)!
        let next = p.gregorian(hijriYear: 1448, month: 10, day: 1, adjustment: 0)!
        let n = Calendar.current.dateComponents([.day], from: first, to: next).day!
        XCTAssertTrue([29, 30].contains(n))
    }
}
