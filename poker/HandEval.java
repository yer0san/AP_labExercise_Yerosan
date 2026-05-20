import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HandEval {

    // Hand rankings (higher = better)
    public static final int HIGH_CARD = 0;
    public static final int PAIR = 1;
    public static final int TWO_PAIR = 2;
    public static final int THREE_OF_A_KIND = 3;
    public static final int STRAIGHT = 4;
    public static final int FLUSH = 5;
    public static final int FULL_HOUSE = 6;
    public static final int FOUR_OF_A_KIND = 7;
    public static final int STRAIGHT_FLUSH = 8;
    public static final int ROYAL_FLUSH = 9;

    public static final String[] HAND_NAMES = {
        "High Card", "Pair", "Two Pair", "Three of a Kind",
        "Straight", "Flush", "Full House", "Four of a Kind",
        "Straight Flush", "Royal Flush"
    };

    // Returns the best score from all 21 combinations of 5 cards out of 7
    public static int bestScore(List<Card> sevenCards) {
        int best = -1;
        List<List<Card>> combos = combinations(sevenCards, 5);
        for (List<Card> five : combos) {
            int score = score(five);
            if (score > best) best = score;
        }
        return best;
    }

    public static int score(List<Card> cards) {
        int[] ranks = new int[5];
        int[] suits = new int[5];
        for (int i = 0; i < 5; i++) {
            ranks[i] = cards.get(i).rank;
            suits[i] = cards.get(i).suit;
        }

        List<Integer> sorted = new ArrayList<>();
        for (int r : ranks) sorted.add(r);
        Collections.sort(sorted, Collections.reverseOrder());

        boolean flush    = isFlush(suits);
        boolean straight = isStraight(sorted);
        int[] counts     = countRanks(ranks); 

        List<Integer> fours  = getRanksWithCount(counts, 4);
        List<Integer> threes = getRanksWithCount(counts, 3);
        List<Integer> pairs  = getRanksWithCount(counts, 2);

        int handRank;
        List<Integer> tiebreakers = new ArrayList<>();

        if (flush && straight && sorted.get(0) == 14) {
            handRank = ROYAL_FLUSH;

        } else if (flush && straight) {
            handRank = STRAIGHT_FLUSH;
            tiebreakers.add(sorted.get(0));

        } else if (!fours.isEmpty()) {
            handRank = FOUR_OF_A_KIND;
            tiebreakers.add(fours.get(0));
            addKickers(tiebreakers, sorted, fours.get(0), 1);

        } else if (!threes.isEmpty() && !pairs.isEmpty()) {
            handRank = FULL_HOUSE;
            tiebreakers.add(threes.get(0));
            tiebreakers.add(pairs.get(0));

        } else if (flush) {
            handRank = FLUSH;
            tiebreakers.addAll(sorted);

        } else if (straight) {
            handRank = STRAIGHT;
            tiebreakers.add(sorted.get(0));

        } else if (!threes.isEmpty()) {
            handRank = THREE_OF_A_KIND;
            tiebreakers.add(threes.get(0));
            addKickers(tiebreakers, sorted, threes.get(0), 2);

        } else if (pairs.size() >= 2) {
            handRank = TWO_PAIR;
            Collections.sort(pairs, Collections.reverseOrder());
            tiebreakers.add(pairs.get(0));
            tiebreakers.add(pairs.get(1));
            addKickers(tiebreakers, sorted, -1, 1); // best kicker not in pairs

        } else if (pairs.size() == 1) {
            handRank = PAIR;
            tiebreakers.add(pairs.get(0));
            addKickers(tiebreakers, sorted, pairs.get(0), 3);

        } else {
            handRank = HIGH_CARD;
            tiebreakers.addAll(sorted);
        }

        return pack(handRank, tiebreakers);
    }

    public static String handName(int score) {
        int handRank = score >> 20;
        return HAND_NAMES[handRank];
    }

    private static boolean isFlush(int[] suits) {
        for (int i = 1; i < 5; i++) if (suits[i] != suits[0]) return false;
        return true;
    }

    private static boolean isStraight(List<Integer> sorted) {
        boolean normal = true;
        for (int i = 0; i < 4; i++) {
            if (sorted.get(i) - sorted.get(i + 1) != 1) { normal = false; break; }
        }
        if (normal) return true;

        List<Integer> wheel = List.of(14, 5, 4, 3, 2);
        return sorted.equals(wheel);
    }

    private static int[] countRanks(int[] ranks) {
        int[] counts = new int[15]; // index 2–14
        for (int r : ranks) counts[r]++;
        return counts;
    }

    private static List<Integer> getRanksWithCount(int[] counts, int n) {
        List<Integer> result = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (counts[r] == n) result.add(r);
        }
        return result;
    }

    private static void addKickers(List<Integer> tiebreakers, List<Integer> sorted,
                                    int exclude, int count) {
        int added = 0;
        for (int r : sorted) {
            if (r != exclude && added < count) {
                tiebreakers.add(r);
                added++;
            }
        }
    }

    private static int pack(int handRank, List<Integer> tiebreakers) {
        int result = handRank << 20;
        for (int i = 0; i < Math.min(tiebreakers.size(), 5); i++) {
            result |= (tiebreakers.get(i) & 0xF) << (16 - i * 4);
        }
        return result;
    }

    private static <T> List<List<T>> combinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        combine(list, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static <T> void combine(List<T> list, int k, int start,
                                     List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            combine(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}