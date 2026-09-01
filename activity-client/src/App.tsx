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
  phase: 'PLAYER' | 'ENEMY'
  status: CombatStatus
}

interface ApiError {
  message?: string
}

const abilities: ReadonlyArray<{
  id: AbilityId
  name: string
  description: string
  target: 'enemy' | 'self'
}> = [
  { id: 'SLASH', name: 'Slash', description: 'Strike one enemy.', target: 'enemy' },
  { id: 'GUARD', name: 'Guard', description: 'Brace behind your shield.', target: 'self' },
]

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

    const ability = abilities.find(({ id }) => id === selectedAbility)
    const targetId = ability?.target === 'self' ? combat.player.entityId : selectedTarget
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
        <p className="eyebrow">Discord Roguelike</p>
        <h1>Ready your shield.</h1>
        <p className="intro">Begin a short Knight encounter against a goblin.</p>
        <button className="primary-button" type="button" onClick={beginCombat} disabled={pending}>
          {pending ? 'Preparing encounter…' : 'Begin encounter'}
        </button>
        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}
      </main>
    )
  }

  const selectedAbilityDetails = abilities.find(({ id }) => id === selectedAbility)
  const canAct = combat.status === 'ACTIVE' && combat.phase === 'PLAYER' && !pending
  const hasTarget = selectedAbilityDetails?.target === 'self' || selectedTarget !== null

  return (
    <main className="combat-shell">
      <header className="combat-header">
        <div>
          <p className="eyebrow">Current encounter</p>
          <h1>Goblin Ambush</h1>
        </div>
        <div className={`status status--${combat.status.toLowerCase()}`} role="status">
          {combat.status === 'ACTIVE' && 'Your turn'}
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
        <article className="player-card">
          <p className="card-label">Knight</p>
          <h2>Your champion</h2>
          <dl className="stats">
            <div>
              <dt>HP</dt>
              <dd>
                {combat.player.currentHp} / {combat.player.maxHp}
              </dd>
            </div>
            <div>
              <dt>Block</dt>
              <dd>{combat.player.block}</dd>
            </div>
          </dl>
        </article>

        <div className="enemy-list">
          {combat.enemies.map((enemy) => {
            const defeated = enemy.currentHp === 0
            return (
              <label
                className={`enemy-card${selectedTarget === enemy.entityId ? ' enemy-card--selected' : ''}${defeated ? ' enemy-card--defeated' : ''}`}
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
                <span className="enemy-card__heading">
                  <span>
                    <span className="card-label">Enemy</span>
                    <strong>{displayName(enemy.contentId)}</strong>
                  </span>
                  <span className="target-marker">{defeated ? 'Defeated' : 'Target'}</span>
                </span>
                <span className="enemy-health">HP {enemy.currentHp} / {enemy.maxHp}</span>
                <span className="intention">
                  <span>Intention</span>
                  <strong>
                    {enemy.intention
                      ? `${displayName(enemy.intention.id)} · ${enemy.intention.damage} damage`
                      : 'None'}
                  </strong>
                </span>
              </label>
            )
          })}
        </div>
      </section>

      {combat.status === 'ACTIVE' && (
        <section className="action-panel" aria-labelledby="ability-heading">
          <div>
            <p className="eyebrow">Choose your move</p>
            <h2 id="ability-heading">Knight abilities</h2>
          </div>
          <div className="ability-list">
            {abilities.map((ability) => (
              <button
                className={`ability-button${selectedAbility === ability.id ? ' ability-button--selected' : ''}`}
                type="button"
                key={ability.id}
                onClick={() => setSelectedAbility(ability.id)}
                disabled={!canAct}
                aria-pressed={selectedAbility === ability.id}
              >
                <strong>{ability.name}</strong>
                <span>{ability.description}</span>
              </button>
            ))}
          </div>
          <button className="primary-button" type="button" onClick={useAbility} disabled={!canAct || !hasTarget}>
            {pending ? 'Resolving turn…' : `Use ${selectedAbilityDetails?.name ?? 'ability'}`}
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
