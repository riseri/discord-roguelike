package dev.riseri.core.combat

object EnemyTurnExecutor {
    private val intentionsByEnemy =
        mapOf(
            EnemyContentId("goblin") to
                listOf(
                    EnemyIntention(IntentionId("stab"), damage = 10),
                ),
            EnemyContentId("goblin-brute") to
                listOf(
                    EnemyIntention(IntentionId("punch"), damage = 8),
                    EnemyIntention(IntentionId("heavy-swing"), damage = 20),
                ),
            EnemyContentId("orc-warlord") to
                listOf(
                    EnemyIntention(IntentionId("slash"), damage = 12),
                    EnemyIntention(IntentionId("crushing-blow"), damage = 30),
                ),
        )

    fun generateIntentions(state: CombatState): ActionResult {
        require(state.status == CombatStatus.ACTIVE) { "Cannot generate intentions for ended combat" }

        var rngState = state.rngState
        val events = mutableListOf<GameEvent>()
        val enemies =
            state.enemies.map { enemy ->
                if (enemy.currentHp.value == 0 || enemy.currentIntention != null) {
                    enemy
                } else {
                    val availableIntentions =
                        intentionsByEnemy[enemy.enemyContentId]
                            ?: error("No intentions defined for ${enemy.enemyContentId.value}")
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

    fun execute(state: CombatState): ActionResult {
        require(state.status == CombatStatus.ACTIVE) { "Cannot execute turns for ended combat" }
        require(state.phase == CombatPhase.ENEMY) { "Enemy turns require the enemy phase" }

        var player = state.player
        val events = mutableListOf<GameEvent>()

        val resolvedEnemies =
            state.enemies.map { enemy ->
                val intention = enemy.currentIntention
                // Once the player is defeated, later enemies must keep their unresolved intentions
                // rather than continuing to act against an already terminal combat.
                if (player.currentHp.value == 0 || enemy.currentHp.value == 0 || intention == null) {
                    enemy
                } else {
                    events +=
                        GameEvent.EnemyActionUsed(
                            enemyId = enemy.entityId,
                            intentionId = intention.id,
                            targetId = player.entityId,
                        )
                    val damageResult = applyDamage(player, enemy.entityId, intention.damage)
                    player = damageResult.player
                    events += damageResult.events
                    enemy.copy(currentIntention = null)
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
                player = player,
                enemies = resolvedEnemies,
                phase = CombatPhase.PLAYER,
            )
        val generated = generateIntentions(afterResolution)

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
