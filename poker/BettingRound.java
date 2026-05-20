import java.util.List;
import java.util.Scanner;

/*
 Runs one complete betting round (pre-flop, flop, turn, or river).
 Rules implemented:
  - Action goes clockwise starting from the correct seat
  - A raise re-opens action to all players who haven't yet responded to it
  - A player can only act if isActive() (not folded, not all-in)
  - Minimum raise = size of the previous raise (or the big blind pre-flop)
  - All-in is handled automatically when a player can't cover a call/raise
 */

public class BettingRound {

    private final GameState state;
    private final Scanner scanner;
    private final int bigBlind;

    public BettingRound(GameState state, Scanner scanner, int bigBlind) {
        this.state = state;
        this.scanner = scanner;
        this.bigBlind = bigBlind;
    }

    public void run(int firstToActIndex, int currentBet) {
        List<Player> players = state.players;
        int n = players.size();

        // Track how much each player has put in THIS round
        int[] roundContrib = new int[n];

        for (int i = 0; i < n; i++) {
            roundContrib[i] = players.get(i).currentBet;
        }

        int lastRaiseSize = (currentBet > 0) ? currentBet : bigBlind;

        boolean[] needsToAct = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (players.get(i).isActive()) needsToAct[i] = true;
        }

        int actionsLeft = countNeedsToAct(needsToAct);
        int current = firstToActIndex;

        while (actionsLeft > 0) {
            if (!needsToAct[current] || !players.get(current).isActive()) {
                current = (current + 1) % n;
                continue;
            }

            Player p = players.get(current);
            int toCall = currentBet - roundContrib[current];

            System.out.println();
            printPlayerStatus(p, toCall, currentBet);

            String action = promptAction(p, toCall, current);

            if (action.equals("fold")) {
                p.folded = true;
                needsToAct[current] = false;
                actionsLeft--;
                System.out.println("  → " + p.name + " folds.");

                // If only one player remains in the hand, end
                if (countActivePlayers() == 1) return;

            } else if (action.equals("check")) {
                needsToAct[current] = false;
                actionsLeft--;
                System.out.println("  → " + p.name + " checks.");

            } else if (action.equals("call")) {
                int paid = Math.min(toCall, p.chips);
                state.collectBet(p, paid);
                roundContrib[current] += paid;
                needsToAct[current] = false;
                actionsLeft--;
                if (p.allIn) {
                    System.out.println("  → " + p.name + " calls " + paid + " and is ALL-IN.");
                } else {
                    System.out.println("  → " + p.name + " calls " + paid + ".");
                }

            } else if (action.startsWith("raise")) {
                int raiseTotal = parseRaiseAmount(action, currentBet, lastRaiseSize, p);
                int raiseBy = raiseTotal - currentBet;
                int toPut = raiseTotal - roundContrib[current];

                toPut = Math.min(toPut, p.chips);
                state.collectBet(p, toPut);
                roundContrib[current] += toPut;

                lastRaiseSize = Math.max(raiseBy, bigBlind);
                currentBet = roundContrib[current]; 

                for (int i = 0; i < n; i++) {
                    if (i != current && players.get(i).isActive()) {
                        if (!needsToAct[i]) {
                            needsToAct[i] = true;
                            actionsLeft++;
                        }
                    }
                }
                needsToAct[current] = false;
                actionsLeft--;

                if (p.allIn) {
                    System.out.println("  → " + p.name + " raises to " + currentBet + " (ALL-IN).");
                } else {
                    System.out.println("  → " + p.name + " raises to " + currentBet + ".");
                }
            }

            current = (current + 1) % n;
        }

        for (Player p : players) {
            p.resetForRound();
        }
    }

    private void printPlayerStatus(Player p, int toCall, int currentBet) {
        System.out.println("┌─ " + p.name + "'s turn  |  Chips: $" + p.chips
                + "  |  Pot: $" + state.pot + "  |  To call: $" + toCall);

        if (!state.community.isEmpty()) {
            System.out.print("│  Board: ");
            for (Card c : state.community) System.out.print(c + " ");
            System.out.println();
        }

        System.out.print("│  Hole cards: ");
        for (Card c : p.holeCards) System.out.print(c + " ");
        System.out.println();
    }

    private String promptAction(Player p, int toCall, int playerIndex) {
        boolean canCheck = (toCall == 0);
        boolean canRaise = (p.chips > toCall);  // must have chips beyond the call

        StringBuilder opts = new StringBuilder("└  Options: ");
        opts.append("[F]old");

        if (canCheck) 
            opts.append("  [C]heck");

        else 
            opts.append("  [C]all $").append(toCall);
        
        if (canRaise) 
            opts.append("  [R]aise");

        System.out.print(opts + "  > ");

        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("f") || input.equals("fold")) {
                return "fold";
            }
            if ((input.equals("c") || input.equals("check")) && canCheck) {
                return "check";
            }
            if ((input.equals("c") || input.equals("call")) && !canCheck) {
                return "call";
            }
            if ((input.startsWith("r")) && canRaise) {
                if (input.equals("r") || input.equals("raise")) {
                    System.out.print("     Raise to (total chips in): ");
                    String amtStr = scanner.nextLine().trim();
                    return "raise " + amtStr;
                }
                return "raise " + input.replaceFirst("^r(aise)?\\s*", "");
            }

            System.out.print("     Invalid input. Try again > ");
        }
    }

    private int parseRaiseAmount(String action, int currentBet,
                                  int lastRaiseSize, Player p) {
        int minRaiseTo = currentBet + lastRaiseSize;
        int maxRaiseTo = currentBet + p.chips;   // all-in cap

        int requested;
        try {
            String[] parts = action.split("\\s+", 2);
            requested = Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            requested = minRaiseTo; // default to min raise on bad input
        }

        // Enforce min raise; cap at all-in
        requested = Math.max(requested, minRaiseTo);
        requested = Math.min(requested, maxRaiseTo);
        return requested;
    }

    private int countNeedsToAct(boolean[] arr) {
        int c = 0;
        for (boolean b : arr) if (b) c++;
        return c;
    }

    private int countActivePlayers() {
        int c = 0;
        for (Player p : state.players) if (!p.folded) c++;
        return c;
    }
}