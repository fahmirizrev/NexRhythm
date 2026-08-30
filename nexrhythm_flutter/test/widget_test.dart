import 'package:flutter_test/flutter_test.dart';
import 'package:nexrhythm_flutter/app/nexrhythm_app.dart';

void main() {
  testWidgets('NexRhythm opens and starts the Trainer', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const NexRhythmApp());

    expect(find.text('NexRhythm'), findsOneWidget);
    expect(find.text('Rhythm Trainer'), findsOneWidget);
    expect(find.text('BPM: 120'), findsOneWidget);
    expect(find.text('Subdivision: 4'), findsOneWidget);
    expect(find.text('Beat: --'), findsOneWidget);
    expect(find.text('Subdivision Step: --'), findsOneWidget);
    expect(find.text('Start'), findsOneWidget);

    await tester.tap(find.text('Start'));
    await tester.pump();

    expect(find.text('Beat: 1'), findsOneWidget);
    expect(find.text('Subdivision Step: 1'), findsOneWidget);
    expect(find.text('Stop'), findsOneWidget);

    await tester.tap(find.text('Stop'));
    await tester.pump();

    expect(find.text('Beat: --'), findsOneWidget);
    expect(find.text('Subdivision Step: --'), findsOneWidget);
    expect(find.text('Start'), findsOneWidget);
  });
}
