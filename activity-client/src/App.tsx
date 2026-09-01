import { useState } from 'react'
import './App.css'

type AbilityId = 'SLASH' | 'GUARD'
type CombatStatus = 'ACTIVE' | 'WON' | 'LOST'

interface PlayerState {
  entityId: string
  currentHp: number
  maxHp: number
  block: number
}

interface EnemyIntention {
  id: string
  damage: number
}

interface EnemyState {
  entityId: string
  contentId: string
  currentHp: number
  maxHp: number
  intention: EnemyIntention | null
}

interface CombatState {
  player: PlayerState
  enemies: EnemyState[]
  abilities: Ability[]
  phase: 'PLAYER' | 'ENEMY'
  status: CombatStatus
}

interface Ability {
  id: AbilityId
  name: string
  description: string
  target: 'ENEMY' | 'SELF'
}

interface ApiError {
  message?: string
}

function displayName(identifier: string) {
  return identifier
    .replaceAll('-', ' ')
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

async function requestCombat(
  path: string,
  body: Record<string, string | number>,
): Promise<CombatState> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ApiError
    throw new Error(error.message || 'The server could not complete that action.')
  }

  return (await response.json()) as CombatState
}

function App() {
  const [combat, setCombat] = useState<CombatState | null>(null)
  const [selectedAbility, setSelectedAbility] = useState<AbilityId>('SLASH')
  const [selectedTarget, setSelectedTarget] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const beginCombat = async () => {
    setPending(true)
    setError(null)
    try {
      const nextCombat = await requestCombat('/api/combat', { seed: 9 })
      setCombat(nextCombat)
      setSelectedTarget(nextCombat.enemies.find((enemy) => enemy.currentHp > 0)?.entityId ?? null)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      setPending(false)
    }
  }

  const useAbility = async () => {
    if (!combat) return

    const ability = combat.abilities.find(({ id }) => id === selectedAbility)
    const targetId = ability?.target === 'SELF' ? combat.player.entityId : selectedTarget
    if (!targetId) {
      setError('Select an enemy target first.')
      return
    }

    setPending(true)
    setError(null)
    try {
      const nextCombat = await requestCombat('/api/combat/actions', {
        abilityId: selectedAbility,
        targetId,
      })
      setCombat(nextCombat)
      const currentTarget = nextCombat.enemies.find(
        (enemy) => enemy.entityId === selectedTarget && enemy.currentHp > 0,
      )
      setSelectedTarget(
        currentTarget?.entityId ??
          nextCombat.enemies.find((enemy) => enemy.currentHp > 0)?.entityId ??
          null,
      )
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      setPending(false)
    }
  }

  if (!combat) {
    return (
      <main className="combat-shell combat-shell--welcome">
        <section className="welcome-window">
          <div className="crest" aria-hidden="true">✦</div>
          <p className="eyebrow">Discord Roguelike</p>
          <h1>Goblin Ambush</h1>
          <p className="intro">Steel your resolve. The old dungeon road is no longer safe.</p>
          <button className="primary-button" type="button" onClick={beginCombat} disabled={pending}>
            {pending ? 'Preparing encounter…' : 'Enter battle'}
          </button>
          {error && <p className="error-message" role="alert">{error}</p>}
        </section>
      </main>
    )
  }

  const selectedAbilityDetails = combat.abilities.find(({ id }) => id === selectedAbility)
  const canAct = combat.status === 'ACTIVE' && combat.phase === 'PLAYER' && !pending
  const hasTarget = selectedAbilityDetails?.target === 'SELF' || selectedTarget !== null

  return (
    <main className="combat-shell">
      <header className="combat-header">
        <div>
          <p className="eyebrow">The Old Dungeon Road</p>
          <h1>Goblin Ambush</h1>
        </div>
        <div className={`status status--${combat.status.toLowerCase()}`} role="status">
          {combat.status === 'ACTIVE' && (pending ? 'Resolving…' : 'Command phase')}
          {combat.status === 'WON' && 'Victory'}
          {combat.status === 'LOST' && 'Defeated'}
        </div>
      </header>

      {combat.status !== 'ACTIVE' && (
        <section className="result-panel">
          <h2>{combat.status === 'WON' ? 'Encounter cleared' : 'The Knight has fallen'}</h2>
          <p>{combat.status === 'WON' ? 'Every enemy has been defeated.' : 'Your run ends here.'}</p>
          <button className="secondary-button" type="button" onClick={beginCombat} disabled={pending}>
            {pending ? 'Preparing…' : 'Start another encounter'}
          </button>
          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
        </section>
      )}

      <section className="battlefield" aria-label="Combatants">
        <div className="battlefield__glow" aria-hidden="true" />
        <article className="player-combatant">
          <div className="fighter fighter--knight" aria-hidden="true">
            <span className="fighter__shield" />
            <span className="fighter__body" />
          </div>
          <section className="status-window player-status" aria-label="Knight status">
            <div className="status-window__heading">
              <div><p className="card-label">Vanguard</p><h2>Knight</h2></div>
              <span className="level-mark">I</span>
            </div>
            <div className="vital-row">
              <div className="vital-row__label"><span>HP</span><strong>{combat.player.currentHp} / {combat.player.maxHp}</strong></div>
              <div className="meter meter--hp" role="meter" aria-label="Knight health" aria-valuemin={0} aria-valuemax={combat.player.maxHp} aria-valuenow={combat.player.currentHp}>
                <span style={{ width: `${(combat.player.currentHp / combat.player.maxHp) * 100}%` }} />
              </div>
            </div>
            <div className="block-stat"><span className="block-stat__icon" aria-hidden="true">◆</span><span>Block</span><strong>{combat.player.block}</strong></div>
          </section>
        </article>

        <div className="enemy-list">
          {combat.enemies.map((enemy) => {
            const defeated = enemy.currentHp === 0
            return (
              <label
                className={`enemy-combatant${selectedTarget === enemy.entityId ? ' enemy-combatant--selected' : ''}${defeated ? ' enemy-combatant--defeated' : ''}`}
                key={enemy.entityId}
              >
                <input
                  type="radio"
                  name="target"
                  value={enemy.entityId}
                  checked={selectedTarget === enemy.entityId}
                  onChange={() => setSelectedTarget(enemy.entityId)}
                  disabled={defeated || !canAct}
                />
                <span className="target-cursor" aria-hidden="true">▼ TARGET</span>
                <span className="intent-banner">
                  <span className="intent-banner__label">Next action</span>
                  <strong>{enemy.intention ? displayName(enemy.intention.id) : 'No action'}</strong>
                  {enemy.intention && <span>{enemy.intention.damage} damage</span>}
                </span>
                <span className="fighter fighter--goblin" aria-hidden="true">
                  <span className="fighter__ear fighter__ear--left" />
                  <span className="fighter__ear fighter__ear--right" />
                  <span className="fighter__body" />
                </span>
                <span className="status-window enemy-status">
                  <span className="status-window__heading">
                    <span><span className="card-label">Enemy</span><strong>{displayName(enemy.contentId)}</strong></span>
                    <span className="target-marker">{defeated ? 'Down' : selectedTarget === enemy.entityId ? 'Selected' : 'Choose'}</span>
                  </span>
                  <span className="vital-row">
                    <span className="vital-row__label"><span>HP</span><strong>{enemy.currentHp} / {enemy.maxHp}</strong></span>
                    <span className="meter meter--enemy" role="meter" aria-label={`${displayName(enemy.contentId)} health`} aria-valuemin={0} aria-valuemax={enemy.maxHp} aria-valuenow={enemy.currentHp}>
                      <span style={{ width: `${(enemy.currentHp / enemy.maxHp) * 100}%` }} />
                    </span>
                  </span>
                </span>
              </label>
            )
          })}
        </div>
      </section>

      {combat.status === 'ACTIVE' && (
        <section className="action-panel" aria-labelledby="ability-heading">
          <div className="command-heading">
            <p className="eyebrow">Knight</p>
            <h2 id="ability-heading">Command</h2>
          </div>
          <div className="ability-list">
            {combat.abilities.map((ability) => (
              <button
                className={`ability-button${selectedAbility === ability.id ? ' ability-button--selected' : ''}`}
                type="button"
                key={ability.id}
                onClick={() => setSelectedAbility(ability.id)}
                disabled={!canAct}
                aria-pressed={selectedAbility === ability.id}
              >
                <span className="command-cursor" aria-hidden="true">▸</span>
                <strong>{ability.name}</strong>
              </button>
            ))}
          </div>
          <div className="command-detail" aria-live="polite">
            <span>{selectedAbilityDetails?.target === 'SELF' ? 'Self' : 'Single enemy'}</span>
            <p>{selectedAbilityDetails?.description}</p>
          </div>
          <button className="primary-button" type="button" onClick={useAbility} disabled={!canAct || !hasTarget}>
            {pending ? 'Resolving…' : 'Confirm'}
          </button>
          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
        </section>
      )}
    </main>
  )
}

export default App
