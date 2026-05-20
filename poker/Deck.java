import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        reset();
    }

    // Rebuild and shuffle a fresh 52-card deck
    public void reset() {
        cards.clear();
        for (int suit = 0; suit < 4; suit++) {
            for (int rank = 2; rank <= 14; rank++) {
                cards.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(cards);
    }

    // Deal one card off the top
    public Card deal() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty!");
        }
        return cards.remove(cards.size() - 1);
    }

    public int size() {
        return cards.size();
    }
}