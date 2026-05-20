import java.util.ArrayList;
import java.util.List;

public class GameState {

    public enum Street { PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN }

    public final List<Player> players;
    public final Deck deck;
    public final List<Card> community;

    public int pot;
    public Street street;
    public int dealerIndex;

    //Constructor

    public GameState(List<Player> players) {
        if (players == null || players.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players.");
        }
        this.players = players;
        this.deck = new Deck();
        this.community = new ArrayList<>();
        this.pot = 0;
        this.street = Street.PRE_FLOP;
        this.dealerIndex = 0;
    }

    /**
    - Prepare everything for a new hand:
    - Reset player state
    - Shuffle a fresh deck
    - Clear community cards and pot
    - Deal 2 hole cards to every active (non-bust) player
     */

    public void startNewHand() {
        community.clear();
        pot    = 0;
        street = Street.PRE_FLOP;
        deck.reset();

        for (Player p : players) {
            p.resetForHand();
        }

        for (Player p : activePlayers()) {
            p.holeCards.add(deck.deal());
            p.holeCards.add(deck.deal());
        }
    }

    public int postBlinds(int smallBlind) {
        int bigBlind = smallBlind * 2;
        List<Player> active = activePlayers();
        int n = active.size();

        Player sb = active.get(1 % n);
        Player bb = active.get(2 % n);

        collectBet(sb, smallBlind);
        collectBet(bb, bigBlind);

        return bigBlind;
    }

    public void dealFlop() {
        deck.deal();
        community.add(deck.deal());
        community.add(deck.deal());
        community.add(deck.deal());
        street = Street.FLOP;
    }

    public void dealTurn() {
        deck.deal(); // burn
        community.add(deck.deal());
        street = Street.TURN;
    }

    public void dealRiver() {
        deck.deal(); // burn
        community.add(deck.deal());
        street = Street.RIVER;
    }

    public void collectBet(Player p, int amount) {
        int actual = Math.min(amount, p.chips);
        p.bet(actual);
        pot += actual;
        if (p.chips == 0) {
            p.allIn = true;
        }
    }

    public List<Player> awardPot() {
        street = Street.SHOWDOWN;

        List<Player> contenders = new ArrayList<>();
        for (Player p : players) {
            if (!p.folded) contenders.add(p);
        }

        if (contenders.size() == 1) {
            contenders.get(0).chips += pot;
            pot = 0;
            return contenders;
        }

        // Score every contender
        int bestScore  = -1;
        for (Player p : contenders) {
            int score = evalPlayer(p);
            if (score > bestScore) bestScore = score;
        }

        List<Player> winners = new ArrayList<>();
        for (Player p : contenders) {
            if (evalPlayer(p) == bestScore) winners.add(p);
        }

        // Split pot odd chip to first winner
        int share    = pot / winners.size();
        int leftover = pot % winners.size();
        for (Player w : winners) {
            w.chips += share;
        }
        winners.get(0).chips += leftover;
        pot = 0;

        return winners;
    }

    public int evalPlayer(Player p) {
        List<Card> seven = new ArrayList<>(p.holeCards);
        seven.addAll(community);
        return HandEval.bestScore(seven);
    }

    public List<Player> activePlayers() {
        List<Player> result = new ArrayList<>();
        for (Player p : players) {
            if (p.chips > 0 || !p.holeCards.isEmpty()) {
                // include anyone dealt into this hand (even all-in)
                if (!p.folded) result.add(p);
            }
        }
        return result;
    }

    public List<Player> actionPlayers() {
        List<Player> result = new ArrayList<>();
        for (Player p : players) {
            if (p.isActive()) result.add(p);
        }
        return result;
    }

    public void advanceDealer() {
        int n = players.size();
        for (int i = 1; i <= n; i++) {
            int next = (dealerIndex + i) % n;
            if (players.get(next).chips > 0) {
                dealerIndex = next;
                return;
            }
        }
    }

    public boolean onlyOnePlayerLeft() {
        int count = 0;
        for (Player p : players) {
            if (p.chips > 0) count++;
        }
        return count <= 1;
    }
}