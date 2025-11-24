package Main;
import java.util.Scanner;
public class Combat {
    Scanner scanner = new Scanner(System.in);
    // initialize participants
    private final Player player;
    private final MechaBeast enemy;
    private boolean playerWon;
    private boolean isTutorial = false;
    private final String stageName;

    public Combat(Player player, MechaBeast enemy, boolean isTutorial, String stageName) {
        this.player = player;
        this.isTutorial = isTutorial;
        this.stageName = stageName != null ? stageName : "";
        this.enemy = prepareEnemy(enemy);
    }

    public boolean getOutcome() {
        return playerWon;
    }

    // Created a nerfed copy for tutorial
    private MechaBeast prepareEnemy(MechaBeast original) {
        try {
            if (!isTutorial) {
                return original.copy();
            }

            int nerfedHp = Math.max(1, original.getMaxHp() / 3);
            int nerfedSpeed = Math.max(1, original.getSpeed() - 20);
            int nerfedMana = Math.max(1, original.getMaxMana() / 2);
            int nerfedManaRegen = Math.max(1, 5);

            MechaBeast nerfed = new MechaBeast(original.getName(), original.getType(), original.getHenshin(),
                    nerfedHp, nerfedSpeed, nerfedMana, nerfedManaRegen);

            Skill[] skills = original.getSkills();
            if (skills != null) {
                for (Skill skill : skills) {
                    if (skill == null) continue;
                    int minP = Math.max(1, (int) Math.round(skill.minPower() * 0.5));
                    int maxP = Math.max(minP, (int) Math.round(skill.maxPower() * 0.5));
                    int manaCost = Math.max(0, skill.manaCost() / 2);
                    nerfed.addSkill(new Skill(skill.name(), skill.type(), minP, maxP, manaCost, skill.cooldown()));
                }
            }
            return nerfed;
        } catch (Exception e) {
            System.out.println("Error preparing enemy: " + e.getMessage());
            return original.copy();
        }
    }

    //Battle mechanics
    public boolean begin() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║              BATTLE START              ║");
        System.out.println("╚════════════════════════════════════════╝");

        if (player != null) player.healAllBeasts();
        if (enemy != null) enemy.fullHeal();

        int round = 1;
        boolean playerFirst = player.getCurrentBeast().getSpeed() >= enemy.getSpeed();

        while (player.hasAliveBeast() && enemy.getCurrentHp() > 0) {

            System.out.println("\n=== " + (stageName.isEmpty() ? "" : (stageName + " - ")) + "ROUND " + round + " ===");
            displayBattleStatus(player.getCurrentBeast(), enemy);

            if(playerFirst) {
                if(playerTurn(player.getCurrentBeast(), enemy)) {
                    return true; // if enemy fainted, skip enemy turn
                } else if (enemyTurn(player.getCurrentBeast(), enemy)) {
                    return false; // if player fainted
                }
            } else {
                if(enemyTurn(player.getCurrentBeast(), enemy)) {
                    return false; // if player fainted, skip player turn
                } else if (playerTurn(player.getCurrentBeast(), enemy)) {
                    return true; // if enemy fainted
                }
            }

            // Skill cooldown reduction after both turns
            player.getCurrentBeast().reduceCooldowns();
            enemy.reduceCooldowns();

            // mana regen
            player.getCurrentBeast().regenerateMana();
            enemy.regenerateMana();

            round++;
        }
        return player.hasAliveBeast(); // if loop ends, return player's surviving status
    }

    //Battle UIs
    private boolean playerTurn(MechaBeast playerBeast, MechaBeast enemyBeast) {
        System.out.println("\n━━━ YOUR TURN ━━━");
        System.out.println("Choose your action:");
        System.out.println("1-3: Use Skill");
        System.out.println("4: Switch Beast");
        System.out.println("5: View Enemy Info");
        System.out.println("6: View Type Chart");

        displaySkills(playerBeast);
        System.out.print("\nYour choice: ");
        int action = getIntInput(6);

        switch(action) {
            case 1:
            case 2:
            case 3:
                int skillIndex = action - 1;
                if (playerBeast.canUseSkill(skillIndex)) {
                    Skill skill = playerBeast.getSkills()[skillIndex];
                    int baseDamage = skill.calculateDamage();

                    // Calculate type effectiveness
                    double effectiveness = skill.type().getEffectivenessAgainst(enemyBeast.getType());
                    int finalDamage = (int) Math.round(baseDamage * effectiveness);

                    System.out.println(" You used " + skill.name() + "!");

                    // Show type advantage message
                    String effectivenessMsg = skill.type().getEffectivenessMessage(enemyBeast.getType());
                    if (!effectivenessMsg.isEmpty()) {
                        System.out.println(" " + effectivenessMsg);
                    }

                    System.out.println(" Enemy took " + finalDamage + " damage!");
                    if (effectiveness != 1.0) {
                        System.out.println("   (Base: " + baseDamage + " × " + effectiveness + "x type bonus)");
                    }

                    enemyBeast.takeDamage(finalDamage);
                    playerBeast.useSkill(skillIndex);

                    // Check if enemy fainted
                    if (!enemyBeast.isAlive()) {
                        System.out.println(" Enemy " + enemyBeast.getName() + " has fainted!");
                        return true;
                    }
                } else {
                    System.out.println(" Skill is unavailable!");
                }
                break;

            case 4:
                System.out.println("\n╔══ SWITCHING BEAST ══╗");
                MechaBeast[] beasts = player.getMechaBeasts();


                // showcase available beasts
                System.out.println(" Choose a beast to switch to:");

                for(int i = 0; i < player.getBeastCount(); i++) {
                    MechaBeast b = beasts[i];

                    // if no beasts in slot
                    if(b == null) {
                        System.out.printf("%d: --- %n", (i + 1));
                        continue;
                    }

                    String status = "";
                    if(b == playerBeast) {
                        status = " [CURRENT]";
                    } else if (!b.isAlive()) {
                        status = " [FAINTED]";
                    }

                    // Displays name, type, status, and HP
                    System.out.printf("%d: %s (%s)%s (HP: %d/%d)%n",
                            (i + 1),
                            beasts[i].getName(),
                            beasts[i].getType().getDisplayName(),
                            status,
                            beasts[i].getCurrentHp(),
                            beasts[i].getMaxHp()
                    );
                }

                // *** FIX APPLIED HERE: Add the prompt for the choice input ***
                System.out.print("Your choice: ");
                
                int choice = getIntInput(player.getBeastCount());
                MechaBeast chosenBeast = beasts[choice - 1];

                if(chosenBeast == playerBeast) {
                    System.out.println(" " + chosenBeast.getName() + " is already in battle!");
                } else if (!chosenBeast.isAlive()) {
                    System.out.println(" " + chosenBeast.getName() + " has fainted and cannot battle!");
                } else {
                    player.setCurrentBeastIndex(choice - 1);
                    System.out.println(" You switched to " + chosenBeast.getName() + "!");
                }

                break;

            case 5:
                displayEnemyInfo(enemyBeast);
                return playerTurn(playerBeast, enemyBeast); // go back to turn


            case 6:
                displayTypeChart();
                return playerTurn(playerBeast, enemyBeast);
        }

        return false;
    }

    /* ga suway rakog make ug ai sa enemy nga ang skill nga isog ang gamiton pero wa pa ni nko na testingan nag suwaysuway rako HAHAHHA
     */

    // Enemy AI turn
    private boolean enemyTurn(MechaBeast playerBeast, MechaBeast enemyBeast) {
        System.out.println("\n━━━ ENEMY TURN ━━━");

        // Simple AI ug unsa ang isog nga skill maoy gamiton
        int skillToUse = -1;
        for (int i = 2; i >= 0; i--) {
            if (enemyBeast.canUseSkill(i)) {
                skillToUse = i;
                break;
            }
        }

        if (skillToUse != -1) {
            Skill skill = enemyBeast.getSkills()[skillToUse];
            int baseDamage = skill.calculateDamage();

            // Calculate type effectiveness
            double effectiveness = skill.type().getEffectivenessAgainst(playerBeast.getType());
            int finalDamage = (int) Math.round(baseDamage * effectiveness);

            System.out.println(" " + enemyBeast.getName() + " used " + skill.name() + "!");

            // Show type advantage message
            String effectivenessMsg = skill.type().getEffectivenessMessage(playerBeast.getType());
            if (!effectivenessMsg.isEmpty()) {
                System.out.println(" " + effectivenessMsg);
            }

            playerBeast.takeDamage(finalDamage);

            System.out.println(" You took " + finalDamage + " damage!");
            if (effectiveness != 1.0) {
                System.out.println("   (Base: " + baseDamage + " × " + effectiveness + "x type bonus)");
            }

            enemyBeast.useSkill(skillToUse);

            if(!playerBeast.isAlive()) {
                System.out.println(" Your " + playerBeast.getName() + " has fainted!");
                return true;
            }

            enemyBeast.reduceMana(skill.manaCost());

        } else {
            System.out.println(enemyBeast.getName() + " is recovering...");
        }


        return false;
    }


    //Victory/defeat conditions
    //ui ra
    private void displayBattleStatus(MechaBeast playerBeast, MechaBeast enemyBeast) {
        System.out.println("\n┌─ YOUR BEAST ─────────────────────────┐");
        System.out.println("│ " + playerBeast.getStatusBar());
        System.out.println("└──────────────────────────────────────┘");

        System.out.println("\n┌─ ENEMY ──────────────────────────────┐");
        System.out.println("│ " + enemyBeast.getStatusBar());
        System.out.println("└──────────────────────────────────────┘");
    }

    //if e type ang 5 mao ni mo gawas
    private void displayEnemyInfo(MechaBeast enemy) {
        System.out.println("\n╔══ ENEMY INFO ══╗");
        System.out.println("Name: " + enemy.getName());
        System.out.println("Type: " + enemy.getType().getDisplayName());
        System.out.println("HP: " + enemy.getCurrentHp() + "/" + enemy.getMaxHp());
        System.out.println("Speed: " + enemy.getSpeed());
        System.out.println("\nSkills:");
        Skill[] skills = enemy.getSkills();
        for (int i = 0; i < 3; i++) {
            if (skills[i] != null) {
                System.out.println("- " + skills[i].getDescription());
            }
        }
        System.out.println("╚═══════════════╝");

    }
    //if e type ang 6 mao ni mo gawas
    private void displayTypeChart() {
        System.out.println("\n╔════════════════ TYPE CHART ════════════════╗");
        System.out.println("║  SUPER EFFECTIVE (2x damage):              ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║ FIRE    → Grass, Wind, Steel               ║");
        System.out.println("║ WATER   → Fire, Earth                      ║");
        System.out.println("║ GRASS   → Water, Earth                     ║");
        System.out.println("║ ELECTRIC→ Water, Wind                      ║");
        System.out.println("║ EARTH   → Electric, Steel, Fire            ║");
        System.out.println("║ WIND    → Grass, Fighting                  ║");
        System.out.println("║ FIGHTING→ Dark, Steel                      ║");
        System.out.println("║ PSYCHIC → Fighting                         ║");
        System.out.println("║ DARK    → Earth, Wind                      ║");
        System.out.println("║ STEEL   → Dark, Electric                   ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║  NOT VERY EFFECTIVE (0.5x damage):         ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║ FIRE    ← Water, Earth                     ║");
        System.out.println("║ WATER   ← Grass, Electric                  ║");
        System.out.println("║ GRASS   ← Fire, Wind                       ║");
        System.out.println("║ ELECTRIC← Earth, Steel                     ║");
        System.out.println("║ EARTH   ← Water, Grass, Dark               ║");
        System.out.println("║ WIND    ← Fire, Electric                   ║");
        System.out.println("║ FIGHTING← Wind, Psychic                    ║");
        System.out.println("║ PSYCHIC ← Dark, Steel                      ║");
        System.out.println("║ DARK    ← Fighting, Steel                  ║");
        System.out.println("║ STEEL   ← Fire, Earth                      ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println("\nTIP: Match your skill type to the enemy's");
        System.out.println("         weakness for maximum damage!");
        pressEnterToContinue();
    }
    private void pressEnterToContinue() {
        System.out.println("\n[Press ENTER to continue]");
        scanner.nextLine();
    }

    //Input validation
    //kani ra gamita if mag input ug number
    private int getIntInput(int max) {
        while (true) {
            try {
                String input = scanner.nextLine();
                int value = Integer.parseInt(input);
                if (value >= 1 && value <= max) {
                    return value;
                }
                System.out.print("Please enter a number between " + 1 + " and " + max + ": ");
            } catch (NumberFormatException e) {

                System.out.print("Invalid input. Please enter a number: ");
            }
        }
    }


    // Display skills for player
    private void displaySkills(MechaBeast beast) {
        // ADDED: Display MechaBeast name and type before the skill list
        System.out.println("\n" + beast.getName() + " TYPE: " + beast.getType().getDisplayName());
        System.out.println("|| Available skills for " + beast.getName() + " ||");
        Skill[] skills = beast.getSkills();

        for (int i = 0; i < skills.length; i++) {
            Skill skill = skills[i];

            if(skill == null) {
                System.out.println((i + 1) + ": ---");
                continue;
            }

            // get mana and cooldown info
            int cd = beast.getSkillCooldown(i);
            boolean onCooldown = cd > 0;
            boolean hasMana = beast.getMana() >= skill.manaCost();
            boolean canUse = hasMana && !onCooldown;


            // display cooldown info
            String cooldownInfo = "";

            if(onCooldown) {
                cooldownInfo = " (Cooldown: " + cd + " more turns)";
            }

            // mana display
            String manaInfo = " | Mana: " + skill.manaCost();

            //skill availability display
            String status = "";

            if(!canUse) {
                if(onCooldown) {
                    status = "[COOLDOWN]";
                } else if (!hasMana) {
                    status = " [INSUFFICIENT MANA]";
                }
            }

            // final output
            System.out.printf("%d: %s (%s) | Power: %d-%d%s%s %s%n",
                    (i + 1), skill.name(), skill.type().getDisplayName(),
                    skill.minPower(), skill.maxPower(),
                    manaInfo, cooldownInfo, status);
        }



    }

    // TUTORIAL MODE METHODS

    // Tutorial mode
    public void setTutorialMode() {
        boolean matchOver = false;
        do {
            matchOver = begin();

            if(isTutorial && !matchOver) {
                System.out.println("Match failed! Starting over...");
                resetCombat();
            }
        } while(isTutorial && !matchOver);
    }

    // Resets combat until player wins
    private void resetCombat() {
        player.healAllBeasts();
        enemy.fullHeal();
    }
}
