package dev.riseri.core.combat

object EnemyTurnExecutor {
    fun generateIntentions(
        state: CombatState,
        enemyDefinitions: Map<EnemyContentId, EnemyDefinition>,
    ): ActionResult {
        require(state.status == CombatStatus.ACTIVE) { "Cannot generate intentions for ended combat" }

        var rngState = state.rngState
        val events = mutableListOf<GameEvent>()
        val enemies =
            state.enemies.map { enemy ->
                // A visible intention is a player-facing commitment. Keeping it also avoids
                // consuming RNG when intention generation is called more than once.
                if (enemy.currentHp.value == 0 || enemy.currentIntention != null) {
                    enemy
                } else {
                    val availableIntentions =
                        enemyDefinitions[enemy.enemyContentId]?.intentions
                            ?: error("No enemy definition found for ${enemy.enemyContentId.value}")
                    val random = rngState.nextInt(availableIntentions.size)
                    rngState = random.nextState
                    val intention = availableIntentions[random.value]
                    events += GameEvent.EnemyIntentionGenerated(enemy.entityId, intention)
                    enemy.copy(currentIntention = intention)
                }
            }

        return ActionResult(
            state = state.copy(enemies = enemies, rngState = rngState),
            events = events,
        )
    }

    fun execute(
        state: CombatState,
        enemyDefinitions: Map<EnemyContentId, EnemyDefinition>,
    ): ActionResult {
        require(state.status == CombatStatus.ACTIVE) { "Cannot execute turns for ended combat" }
        require(state.phase == CombatPhase.ENEMY) { "Enemy turns require the enemy phase" }

        var player = state.player
        val events = mutableListOf<GameEvent>()

        // List order is combat order. This makes multi-enemy resolution deterministic and ensures
        // the first lethal action prevents every later enemy from acting.
        val resolvedEnemies = mutableListOf<EnemyCombatState>()
        state.enemies.forEachIndexed { enemyIndex, enemy ->
            val intention = enemy.currentIntention
            // Once the player is defeated, later enemies must keep their unresolved intentions
            // rather than continuing to act against an already terminal combat.
            if (player.currentHp.value == 0 || enemy.currentHp.value == 0 || intention == null) {
                resolvedEnemies += enemy
            } else if (enemy.stunnedTurns > 0) {
                resolvedEnemies += enemy.copy(stunnedTurns = enemy.stunnedTurns - 1, currentIntention = null)
            } else {
                var actingEnemy = enemy
                if (TacticalMovement.distance(enemy.position, player.position) > 1) {
                    val occupied =
                        buildSet {
                            add(player.position)
                            state.enemies
                                .filterIndexed { index, other -> index > enemyIndex && other.currentHp.value > 0 }
                                .forEach { add(it.position) }
                            resolvedEnemies.filter { it.currentHp.value > 0 }.forEach { add(it.position) }
                        }
                    val goals = TacticalMovement.adjacent(player.position).filter(state.grid::contains).toSet()
                    val path = TacticalMovement.path(state.grid, enemy.position, goals, occupied)
                    val destination = path?.take(CombatState.ENEMY_MOVEMENT)?.lastOrNull()
                    if (destination != null) {
                        events += GameEvent.EntityMoved(enemy.entityId, enemy.position, destination)
                        actingEnemy = enemy.copy(position = destination)
                    }
                }
                if (TacticalMovement.distance(actingEnemy.position, player.position) == 1) {
                    events +=
                        GameEvent.EnemyActionUsed(
                            enemyId = enemy.entityId,
                            intentionId = intention.id,
                            targetId = player.entityId,
                        )
                    val damageResult = applyDamage(player, enemy.entityId, intention.damage)
                    player = damageResult.player
                    events += damageResult.events
                }
                resolvedEnemies += actingEnemy.copy(currentIntention = null)
            }
        }

        if (player.currentHp.value == 0) {
            return ActionResult(
                state =
                    state.copy(
                        player = player,
                        enemies = resolvedEnemies,
                        status = CombatStatus.LOST,
                    ),
                events =
                    events +
                        listOf(
                            GameEvent.EntityDefeated(player.entityId),
                            GameEvent.CombatLost,
                        ),
            )
        }

        val afterResolution =
            state.copy(
                player = player.copy(movedThisPhase = false),
                enemies = resolvedEnemies,
                phase = CombatPhase.PLAYER,
            )
        // Generate the next committed intentions only after every current intention has resolved,
        // making them visible for the player's newly started turn.
        val generated = generateIntentions(afterResolution, enemyDefinitions)

        return ActionResult(
            state = generated.state,
            events = events + generated.events,
        )
    }

    private fun applyDamage(
        player: PlayerCombatState,
        sourceId: EntityId,
        incomingDamage: Int,
    ): DamageResult {
        // Damage and events follow the documented pipeline: Block is consumed before HP, and HP
        // damage is clamped so externally visible health never becomes negative.
        val blockAbsorbed = minOf(player.block.value, incomingDamage)
        val remainingDamage = incomingDamage - blockAbsorbed
        val hpDamage = minOf(player.currentHp.value, remainingDamage)
        val events = mutableListOf<GameEvent>()

        if (blockAbsorbed > 0) {
            events += GameEvent.BlockAbsorbed(player.entityId, blockAbsorbed)
        }
        if (hpDamage > 0) {
            events += GameEvent.DamageDealt(sourceId, player.entityId, hpDamage)
        }

        return DamageResult(
            player =
                player.copy(
                    currentHp = HitPoints(player.currentHp.value - hpDamage),
                    block = Block(player.block.value - blockAbsorbed),
                ),
            events = events,
        )
    }

    private data class DamageResult(
        val player: PlayerCombatState,
        val events: List<GameEvent>,
    )
}
