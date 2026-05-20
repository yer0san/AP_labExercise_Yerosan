import java.util.ArrayList;
import java.util.List;

public class Player {

    public final String name;
    public int chips;
    public List<Card> holeCards = new ArrayList<>();
    public int currentBet;
    public boolean folded;
    public boolean allIn;

    public Player(String name, int chips) {
        this.name = name;
        this.chips = chips;
    }

    public void bet(int amount) {
        amount = Math.min(amount, chips);
        chips -= amount;
        currentBet += amount;
    }

    public void resetForRound() {
        currentBet = 0;
    }

    public void resetForHand() {
        holeCards.clear();
        currentBet = 0;
        folded = false;
        allIn = false;
    }

    public boolean isActive() {
        return !folded && !allIn;
    }

    @Override
    public String toString() {
        return name + " ($" + chips + ")";
    }
}