import { useRef, useState } from 'react'
import { ShieldIcon, SwordIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import './App.css'
import './components/game-ui/GameUi.css'
import {
  CombatantStatus,
  CommandButton,
  CommandMenu,
  EnemyIntent,
  JrpgWindow,
  ResultWindow,
  TargetButton,
  TargetCursor,
} from './components/game-ui/GameUi'

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

type CombatEvent = {
  type: 'ABILITY_USED' | 'DAMAGE_DEALT' | 'BLOCK_GAINED' | 'ENEMY_INTENTION_GENERATED' |
    'ENEMY_ACTION_USED' | 'BLOCK_ABSORBED' | 'ENTITY_DEFEATED' | 'COMBAT_WON' | 'COMBAT_LOST'
  actorId?: string
  abilityId?: AbilityId
  sourceId?: string
  targetId?: string
  entityId?: string
  enemyId?: string
  intention?: EnemyIntention
  intentionId?: string
  amount?: number
}

interface CombatActionResponse {
  state: CombatState
  events: CombatEvent[]
}

function displayName(identifier: string) {
  return identifier
    .replaceAll('-', ' ')
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

async function requestCombat<T>(
  path: string,
  body: Record<string, string | number>,
): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ApiError
    throw new Error(error.message || 'The server could not complete that action.')
  }

  return (await response.json()) as T
}

const messageDelay = 360
const stateChangeDelay = 220

function waitForPresentation(delay: number) {
  return new Promise((resolve) => window.setTimeout(resolve, delay))
}

// Event amounts are authoritative deltas, allowing the UI to reveal state without replaying rules.
function applyEvent(state: CombatState, event: CombatEvent): CombatState {
  const amount = event.amount ?? 0
  if (event.type === 'DAMAGE_DEALT' && event.targetId) {
    if (event.targetId === state.player.entityId) {
      return { ...state, player: { ...state.player, currentHp: Math.max(0, state.player.currentHp - amount) } }
    }
    return {
      ...state,
      enemies: state.enemies.map((enemy) => enemy.entityId === event.targetId
        ? { ...enemy, currentHp: Math.max(0, enemy.currentHp - amount) }
        : enemy),
    }
  }
  if (event.type === 'BLOCK_GAINED' && event.entityId === state.player.entityId) {
    return { ...state, player: { ...state.player, block: state.player.block + amount } }
  }
  if (event.type === 'BLOCK_ABSORBED' && event.entityId === state.player.entityId) {
    return { ...state, player: { ...state.player, block: Math.max(0, state.player.block - amount) } }
  }
  if (event.type === 'ENEMY_INTENTION_GENERATED' && event.enemyId && event.intention) {
    return {
      ...state,
      enemies: state.enemies.map((enemy) => enemy.entityId === event.enemyId
        ? { ...enemy, intention: event.intention ?? null }
        : enemy),
    }
  }
  if (event.type === 'ENEMY_ACTION_USED' && event.enemyId) {
    return {
      ...state,
      phase: 'ENEMY',
      enemies: state.enemies.map((enemy) => enemy.entityId === event.enemyId
        ? { ...enemy, intention: null }
        : enemy),
    }
  }
  if (event.type === 'COMBAT_WON') return { ...state, status: 'WON' }
  if (event.type === 'COMBAT_LOST') return { ...state, status: 'LOST' }
  return state
}

function eventMessage(event: CombatEvent, state: CombatState) {
  if (event.type === 'ABILITY_USED') return `Knight uses ${displayName(event.abilityId ?? 'ability')}`
  if (event.type === 'DAMAGE_DEALT') return `${event.amount ?? 0} damage`
  if (event.type === 'BLOCK_GAINED') return `Guard raised · +${event.amount ?? 0} Block`
  if (event.type === 'BLOCK_ABSORBED') return `Block absorbs ${event.amount ?? 0}`
  if (event.type === 'ENEMY_ACTION_USED') {
    const enemy = state.enemies.find(({ entityId }) => entityId === event.enemyId)
    return `${displayName(enemy?.contentId ?? 'Enemy')} uses ${displayName(event.intentionId ?? 'Attack')}`
  }
  if (event.type === 'ENTITY_DEFEATED') return event.entityId === state.player.entityId ? 'Knight falls' : 'Enemy defeated'
  if (event.type === 'COMBAT_WON') return 'Victory!'
  if (event.type === 'COMBAT_LOST') return 'Defeated'
  return null
}

function App() {
  const [combat, setCombat] = useState<CombatState | null>(null)
  const [selectedAbility, setSelectedAbility] = useState<AbilityId>('SLASH')
  const [selectedTarget, setSelectedTarget] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const [presentation, setPresentation] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const actionInFlight = useRef(false)

  const beginCombat = async () => {
    setPending(true)
    setError(null)
    try {
      const nextCombat = await requestCombat<CombatState>('/api/combat', { seed: 9 })
      setCombat(nextCombat)
      setSelectedTarget(nextCombat.enemies.find((enemy) => enemy.currentHp > 0)?.entityId ?? null)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      setPending(false)
    }
  }

  const useAbility = async () => {
    if (!combat || actionInFlight.current) return

    const ability = combat.abilities.find(({ id }) => id === selectedAbility)
    const targetId = ability?.target === 'SELF' ? combat.player.entityId : selectedTarget
    if (!targetId) {
      setError('Select an enemy target first.')
      return
    }

    actionInFlight.current = true
    setPending(true)
    setError(null)
    try {
      const result = await requestCombat<CombatActionResponse>('/api/combat/actions', {
        abilityId: selectedAbility,
        targetId,
      })
      let displayedCombat = combat
      for (const event of result.events) {
        const message = eventMessage(event, displayedCombat)
        if (message) {
          setPresentation(message)
          await waitForPresentation(messageDelay)
        }
        displayedCombat = applyEvent(displayedCombat, event)
        setCombat(displayedCombat)
        if (message) await waitForPresentation(stateChangeDelay)
      }
      setCombat(result.state)
      setPresentation(null)
      const currentTarget = result.state.enemies.find(
        (enemy) => enemy.entityId === selectedTarget && enemy.currentHp > 0,
      )
      setSelectedTarget(
        currentTarget?.entityId ??
          result.state.enemies.find((enemy) => enemy.currentHp > 0)?.entityId ??
          null,
      )
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Unable to reach the game server.')
    } finally {
      actionInFlight.current = false
      setPresentation(null)
      setPending(false)
    }
  }

  if (!combat) {
    return (
      <main className="combat-shell combat-shell--welcome">
        <JrpgWindow as="section" className="welcome-window">
          <div className="crest" aria-hidden="true">✦</div>
          <p className="eyebrow">Discord Roguelike</p>
          <h1>Goblin Ambush</h1>
          <p className="intro">Steel your resolve. The old dungeon road is no longer safe.</p>
          <Button className="primary-button" type="button" onClick={beginCombat} disabled={pending}>
            {pending ? 'Preparing encounter…' : 'Enter battle'}
          </Button>
          {error && <p className="error-message" role="alert">{error}</p>}
        </JrpgWindow>
      </main>
    )
  }

  const selectedAbilityDetails = combat.abilities.find(({ id }) => id === selectedAbility)
  const canAct = combat.status === 'ACTIVE' && combat.phase === 'PLAYER' && !pending
  const hasTarget = selectedAbilityDetails?.target === 'SELF' || selectedTarget !== null

  return (
    <main className="combat-shell">
      <JrpgWindow as="header" className="combat-header">
        <div className="combat-header__title">
          <h1>Goblin Ambush</h1>
          <p>The Old Dungeon Road</p>
        </div>
        <div className={`phase-badge phase-badge--${combat.status.toLowerCase()}`} role="status">
          {combat.status === 'ACTIVE' && (presentation ?? (pending ? 'Resolving…' : 'Your turn'))}
          {combat.status === 'WON' && 'Victory'}
          {combat.status === 'LOST' && 'Defeated'}
        </div>
        <span className="encounter-kind">Solo encounter</span>
      </JrpgWindow>

      {combat.status !== 'ACTIVE' && <ResultWindow status={combat.status} pending={pending} onRestart={beginCombat} />}

      <section className="battlefield" aria-label="Combatants">
        <div className="battlefield__glow" aria-hidden="true" />
        <div className="battlefield-stage">
          <article className="player-combatant" aria-label="Knight">
            <div className="fighter fighter--knight" aria-hidden="true">
              <span className="fighter__shield" />
              <span className="fighter__body" />
            </div>
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
                  <div className="enemy-presentation">
                    <TargetCursor selected={selectedTarget === enemy.entityId} />
                    <EnemyIntent
                      name={enemy.intention ? displayName(enemy.intention.id) : 'No action'}
                      damage={enemy.intention?.damage}
                    />
                    <span className="fighter fighter--goblin" aria-hidden="true">
                      <span className="fighter__ear fighter__ear--left" />
                      <span className="fighter__ear fighter__ear--right" />
                      <span className="fighter__body" />
                    </span>
                  </div>
                </label>
              )
            })}
          </div>
        </div>

        <div className="battlefield-status-row">
          <CombatantStatus
            className="player-status"
            name="Knight"
            label="You"
            currentHp={combat.player.currentHp}
            maxHp={combat.player.maxHp}
            hpLabel="Knight health"
            tone="player"
            block={combat.player.block}
            marker={<span className="level-mark"><ShieldIcon size={16} /></span>}
          />
          <div className="enemy-status-list">
            {combat.enemies.map((enemy) => {
              const defeated = enemy.currentHp === 0
              return (
                <CombatantStatus
                  key={enemy.entityId}
                  className="enemy-status"
                  name={displayName(enemy.contentId)}
                  label="Enemy"
                  currentHp={enemy.currentHp}
                  maxHp={enemy.maxHp}
                  hpLabel={`${displayName(enemy.contentId)} health`}
                  tone="enemy"
                  marker={
                    <span className="target-marker">
                      {defeated ? 'Down' : selectedTarget === enemy.entityId ? 'Selected' : 'Choose'}
                    </span>
                  }
                />
              )
            })}
          </div>
        </div>
      </section>

      {combat.status === 'ACTIVE' && (
        <JrpgWindow as="section" className="action-panel" aria-labelledby="ability-heading">
          <div className="command-menu">
            <p className="panel-prompt" id="ability-heading">Choose an action</p>
            <CommandMenu
              value={selectedAbility}
              onValueChange={(value) => setSelectedAbility(value as AbilityId)}
              disabled={!canAct}
            >
              {combat.abilities.map((ability) => (
                <CommandButton
                  key={ability.id}
                  value={ability.id}
                  icon={ability.id === 'GUARD' ? <ShieldIcon size={17} /> : <SwordIcon size={17} />}
                >
                  {ability.name}
                </CommandButton>
              ))}
            </CommandMenu>
          </div>
          <div className="command-detail" aria-live="polite">
            <div className="command-detail__title">
              {selectedAbility === 'GUARD' ? <ShieldIcon size={24} /> : <SwordIcon size={24} />}
              <h2>{selectedAbilityDetails?.name}</h2>
            </div>
            <p>{selectedAbilityDetails?.description}</p>
            <Separator className="command-separator" />
            <dl>
              <div><dt>Target</dt><dd>{selectedAbilityDetails?.target === 'SELF' ? 'Self' : 'One enemy'}</dd></div>
              <div><dt>Type</dt><dd>{selectedAbility === 'GUARD' ? 'Defense' : 'Attack'}</dd></div>
            </dl>
          </div>
          <div className="target-menu">
            <p className="panel-prompt">{selectedAbilityDetails?.target === 'SELF' ? 'Target' : 'Select target'}</p>
            {selectedAbilityDetails?.target === 'SELF' ? (
              <TargetButton name="Knight" selected disabled={!canAct} onSelect={() => undefined} />
            ) : combat.enemies.map((enemy) => (
              <TargetButton
                key={enemy.entityId}
                name={displayName(enemy.contentId)}
                selected={selectedTarget === enemy.entityId}
                disabled={!canAct || enemy.currentHp === 0}
                onSelect={() => setSelectedTarget(enemy.entityId)}
              />
            ))}
            <div className="target-menu__footer">
              <Button className="primary-button" type="button" onClick={useAbility} disabled={!canAct || !hasTarget}>
                {pending ? 'Resolving…' : 'Confirm action'}
              </Button>
            </div>
          </div>
          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
        </JrpgWindow>
      )}
    </main>
  )
}

export default App
