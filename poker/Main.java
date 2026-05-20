import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
   1. Setup (player count, names, starting chips)
   2. Hand loop: deal → pre-flop → flop → turn → river → showdown
   3. Post-hand: display results, remove bust players, continue or end
 */
public class Main {
    private static final int STARTING_CHIPS = 1000;
    private static final int SMALL_BLIND = 10;
    private static final int BIG_BLIND = SMALL_BLIND * 2;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        printBanner();

        List<Player> players = setupPlayers(scanner);
        GameState state = new GameState(players);

        System.out.println("\nGame starting with "
                + players.size() + " players, $" + STARTING_CHIPS + " each.");
        System.out.println("Blinds: $" + SMALL_BLIND + " / $" + BIG_BLIND);

        //Main game loop

        int handNumber = 1;
        while (!state.onlyOnePlayerLeft()) {

            System.out.println("\n" + "═".repeat(56));
            System.out.println("  HAND #" + handNumber
                    + "  |  Dealer: " + state.players.get(state.dealerIndex).name);
            System.out.println("═".repeat(56));

            playHand(state, scanner);

            removeBustPlayers(state, players);

            if (state.onlyOnePlayerLeft()) break;

            state.advanceDealer();
            handNumber++;

            System.out.print("\nPress ENTER to deal the next hand...");
            scanner.nextLine();
        }

        printWinner(players);
        scanner.close();
    }

    private static void playHand(GameState state, Scanner scanner) {
        state.startNewHand();

        int bbAmount = state.postBlinds(SMALL_BLIND);

        System.out.println("\n── PRE-FLOP ──");
        printChipCounts(state.players);

        int preflop = firstToActPreFlop(state);
        BettingRound preFlop = new BettingRound(state, scanner, BIG_BLIND);
        preFlop.run(preflop, bbAmount);

        if (handOver(state)) { showdown(state); return; }

        state.dealFlop();
        System.out.println("\n── FLOP ──  " + communityString(state));
        BettingRound flop = new BettingRound(state, scanner, BIG_BLIND);
        flop.run(firstToActPostFlop(state), 0);

        if (handOver(state)) { showdown(state); return; }

        state.dealTurn();
        System.out.println("\n── TURN ──  " + communityString(state));
        BettingRound turn = new BettingRound(state, scanner, BIG_BLIND);
        turn.run(firstToActPostFlop(state), 0);

        if (handOver(state)) { showdown(state); return; }

        state.dealRiver();
        System.out.println("\n── RIVER ──  " + communityString(state));
        BettingRound river = new BettingRound(state, scanner, BIG_BLIND);
        river.run(firstToActPostFlop(state), 0);

        showdown(state);
    }

    private static void showdown(GameState state) {
        System.out.println("\n── SHOWDOWN ──");
        System.out.println("  Board: " + communityString(state));
        System.out.println("  Pot:   $" + state.pot);

        List<Player> contenders = state.activePlayers();
        if (contenders.size() > 1) {
            System.out.println();
            for (Player p : contenders) {
                int score    = state.evalPlayer(p);
                String hname = HandEval.handName(score);
                System.out.println("  " + p.name + ": "
                        + holeCardsString(p) + "  →  " + hname);
            }
        }

        List<Player> winners = state.awardPot();

        System.out.println();
        if (winners.size() == 1) {
            Player w = winners.get(0);
            String handDesc = (contenders.size() > 1)
                    ? " with " + HandEval.handName(state.evalPlayer(w))
                    : " (everyone else folded)";
            System.out.println("  🏆  " + w.name + " wins the pot" + handDesc + "!");
        } else {
            StringBuilder sb = new StringBuilder("  🤝  Split pot — ");
            for (int i = 0; i < winners.size(); i++) {
                sb.append(winners.get(i).name);
                if (i < winners.size() - 1) sb.append(" & ");
            }
            System.out.println(sb);
        }

        System.out.println();
        printChipCounts(state.players);
    }

    private static List<Player> setupPlayers(Scanner scanner) {
        int count = 0;
        while (count < 2 || count > 9) {
            System.out.print("How many players? (2-9): ");
            try {
                count = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                count = 0;
            }
            if (count < 2 || count > 9) {
                System.out.println("  Please enter a number between 2 and 9.");
            }
        }

        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            System.out.print("Name for Player " + i + ": ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = "Player " + i;
            players.add(new Player(name, STARTING_CHIPS));
        }
        return players;
    }

    private static void removeBustPlayers(GameState state, List<Player> players) {
        List<Player> busted = new ArrayList<>();
        for (Player p : players) {
            if (p.chips == 0) busted.add(p);
        }
        for (Player p : busted) {
            System.out.println("  " + p.name + " is eliminated (out of chips).");
            state.players.remove(p);
        }
    }

    private static int firstToActPreFlop(GameState state) {
        int n = state.players.size();
        if (n == 2) 
            return state.dealerIndex;
        return (state.dealerIndex + 3) % n;
    }

    private static int firstToActPostFlop(GameState state) {
        int n = state.players.size();
        for (int i = 1; i <= n; i++) {
            int idx = (state.dealerIndex + i) % n;
            if (state.players.get(idx).isActive()) return idx;
        }
        return state.dealerIndex;
    }

    private static boolean handOver(GameState state) {
        int active = 0;
        for (Player p : state.players) if (!p.folded) active++;
        return active <= 1;
    }


    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║                   TEXAS HOLD'EM                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void printChipCounts(List<Player> players) {
        System.out.println("  Chip counts:");
        for (Player p : players) {
            String status = p.chips == 0 ? "  [BUST]" : "";
            System.out.println("    " + p.name + ": $" + p.chips + status);
        }
    }

    private static void printWinner(List<Player> players) {
        System.out.println("\n" + "═".repeat(56));
        System.out.println("  GAME OVER");
        for (Player p : players) {
            if (p.chips > 0) {
                System.out.println("  Champion: " + p.name
                        + " with $" + p.chips + "!");
            }
        }
        System.out.println("═".repeat(56));
    }

    private static String communityString(GameState state) {
        if (state.community.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (Card c : state.community) sb.append(c).append(" ");
        return sb.toString().trim();
    }

    private static String holeCardsString(Player p) {
        StringBuilder sb = new StringBuilder();
        for (Card c : p.holeCards) sb.append(c).append(" ");
        return sb.toString().trim();
    }
}