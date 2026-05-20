public class Card {

    // Ranks: 2–14 (11=Jack, 12=Queen, 13=King, 14=Ace)
    public static final String[] SUITS = {" SPADES", " HEARTS", " DIAMONDS", " CLUBS"};
    public static final String[] RANK_NAMES = {
        "", "", "2", "3", "4", "5", "6", "7",
        "8", "9", "10", "J", "Q", "K", "A"
    };

    public final int rank;  // 2–14
    public final int suit;  // 0=Spades, 1=Hearts, 2=Diamonds, 3=Clubs

    public Card(int rank, int suit) {
        this.rank = rank;
        this.suit = suit;
    }

    @Override
    public String toString() {
        return RANK_NAMES[rank] + SUITS[suit];
    }
}