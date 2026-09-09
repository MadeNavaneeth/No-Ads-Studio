---
status: canonical
---

# Nothing — Deep Study

What they do, why they do it, what is actually unique, and where our canon disagrees with them.

Sourced from Nothing's design leadership on record, analysis of their shipping products, and their
own developer materials. Everything here is cited. Where a claim is my reading rather than theirs,
it says so.

---

## 1. The core idea is demystification, not minimalism

This is the single most misread thing about Nothing. Copying them as "minimalist monochrome" gets the
surface and misses the engine.

Adam Bates, their design director, on transparency: it is a deep concept for them, about
[demystifying tech so people can see the engineering and see the function](https://hypebeast.com/2023/9/nothing-design-labs-london-design-festival-installation).
The transparent back is not a styling choice. It is an argument that hidden machinery should be
visible.

He is explicit that this costs them: transparency
[significantly increases the design workload](https://issuu.com/disegnomagazine/docs/d34_book_1_/s/16709786),
because surfaces, finishes, and components that are normally hidden are suddenly on show. A flexible
PCB is not beautiful by default — they had to work to make it so.

Creative Bloq's read is that Nothing
[designs its phones like logos](https://www.creativebloq.com/design/product-design/why-nothing-designs-its-phones-like-logos):
screws and components are treated as graphic elements, closer to product graphics than to the
anonymous minimalism of every other phone.

**What this means for a puzzle game.** Minimalism says hide everything inessential. Nothing says
*expose the mechanism and make it beautiful*. Those produce very different Sudoku apps.

- Don't hide the generator. Show difficulty, seed, generation state as readouts.
- Don't hide why a cell is wrong. Show the conflicting row, column, or box.
- The loading state is not something to skip past. It is a chance to show the machine working.
- Give the timer, the move count, and the completion percentage the status of instruments, not chrome.

A Sudoku that exposes its own logic is more Nothing than one that politely conceals it.

---

## 2. Teenage Engineering is a co-founder, which explains everything

Nothing was founded in 2020 by Carl Pei **and Teenage Engineering** — the Swedish electronics company
behind the OP-1 — [per Disegno](https://issuu.com/disegnomagazine/docs/d34_book_1_/99). Not an agency
they hired. A co-founder.

This is why Nothing products feel like *instruments* rather than appliances. Teenage Engineering's
grammar is all over it: numbered and lettered controls, functional labels printed directly on the
surface, no ornament, colour used as a functional code rather than decoration, and a willingness to
be strange in service of clarity.

The other lineage is Dyson. Bates joined Nothing in early 2022 after
[14 years at Dyson](https://www.dezeen.com/2024/02/20/nothing-tech-smartphone-design-adam-bates-interview/),
and Dyson's whole method is making engineering visible and treating function as the aesthetic.

**Application.** Aim for instrument panel, not app. That single reframe drives most of the right
decisions: labels instead of icons, monospace readouts instead of prose, state always visible,
nothing decorative.

---

## 3. The Glyph is functional light, and this is the most transferable idea

Bates on what the Glyph was for: it challenged the assumption that a phone must always command your
attention, and introduced a
[visual language of light with ambient cues](https://design-milk.com/the-nothing-phone-3s-glyph-matrix-turns-notifications-into-pixel-art/)
that let you stay present while still staying informed.

Nothing describe the Glyph Matrix as
[designed to communicate without pulling you in](https://www.forbes.com/sites/prakharkhanna/2025/07/01/glyph-matrix-on-nothing-phone-3-everything-you-can-do-with-the-new-interface/).
Every Glyph use has been functional: flash, timer, progress bar, notification, charging state.

The dot matrix in their typography has the same root. NDot builds each character from circular dots
[like a transit departure board](https://www.shadcn.io/design/nothing) — a reference to a display
whose entire purpose is conveying information at a glance.

**This is the finding that matters most for us.** In Nothing's language, dots always *mean* something.
A decorative dot grid is the single clearest way to reveal you have copied the look without
understanding it.

So the dot matrix in our game should be a readout:

- Dot density or dot size tracking completion
- A ring of dots as the timer
- A short row of dots for mistakes remaining
- Dots revealing which cells are still deducible

Doto's variable dot-size axis makes this genuinely easy, which is a second reason to prefer it over
the static NDot files.

---

## 4. Colour: the finding that contradicts our canon

Our canon mandates one red accent per screen. Nothing's own website has **no accent colour at all**.

Analysis of their site describes the palette as total monochrome — black, white, and a mid-grey — with
[no accent, no brand colour, and no interactive highlight](https://www.shadcn.io/design/nothing). The
primary call to action is a black rectangle with white uppercase mono text at 11px.

Nothing's red lives in brand, packaging, and product contexts. It is not a UI accent colour.

**Correction to our rule.** "At most one red element per screen" is technically compatible, but the
spirit is wrong. It reads as *aim for one*. The truthful rule is:

> Default to zero accent. Monochrome carries the entire interface. Red appears only when something is
> actually wrong, and its rarity is the whole point of its power.

A Sudoku screen in normal play should have **no red on it**. Red on a conflict is the exception that
proves the system, not a decorative budget to spend.

Two related observations from the same analysis, both places where our canon is stricter than Nothing
themselves:

- Their primary web CTA is a **rectangle**, not a pill. Our "no sharp corners anywhere" rule is more
  absolute than Nothing's practice. Their OS does use rounded forms, so this is a web-versus-OS
  difference, but the absolutism is ours, not theirs.
- Their headlines run at **weight 100**, ultra-light, so type recedes and imagery dominates. Our
  canon allows Light but does not push nearly that far. For a game with no imagery, heavier is
  probably right — but the instinct to let type recede is worth stealing.

---

## 5. Nothing in 2026 is moving away from austerity

This is a strategic finding, and you should make a deliberate choice about it.

Nothing OS 5.0, announced August 2026, introduces Geist as the system typeface plus a
[frosted translucent material across Settings, status bar, Camera, and Gallery](https://www.wirefly.com/news/open-beta-nothing-os-50-starts-tomorrow-just-not-everyone),
with app icons and widgets picking up colour tints pulled from the wallpaper. Widgets now sit on the
wallpaper
[with a sense of depth rather than floating flat](https://www.digitaltrends.com/phones/nothing-os-5-0-wants-you-to-stop-switching-apps-and-start-living-in-your-phone/).

Geist was chosen for clarity, character, and consistency — legible at small sizes in Settings while
still [carrying enough personality to feel distinctly Nothing](https://techgenyz.com/nothing-os-5-0-great-changes-revealed/).

Meanwhile Carl Pei frames the whole industry as
[quieter, more minimal, more monotonous](https://lbbonline.com/news/Nothing-Is-on-Mission-to-Bring-Maximalism-and-Personality-to-Tech),
and positions Nothing as bringing maximalism and personality back. Domus describes their current
design language as built on
[colour, transparency, and luminous interfaces](https://www.domusweb.it/en/news/2026/03/05/nothing-phone-4a-headphone-a-design-tech-carl-pei.html),
and Wallpaper has covered their [evolving use of colour](https://www.wallpaper.com/tech/nothing-design-team-interview)
as newer products open up a broader palette.

**So our canon bans blur, gradients, depth, and colour — and Nothing OS 5.0 now uses translucency,
depth, and wallpaper-derived tints.**

Our canon is Nothing circa 2022–2023: the austere, flat, monochrome era.

**My recommendation: stay in that era deliberately, and say so.** For a puzzle game, calm and flat is
the right call — a Sudoku grid does not want translucency competing with it, and flat monochrome is
still instantly recognisable as Nothing. But this should be a stated position ("we target the austere
Nothing OS 2.x aesthetic") rather than an accident of when the research was done. Otherwise the canon
silently rots as Nothing keeps moving.

---

## 6. What is genuinely unique to them

Stripped of everything other minimal brands also do, this is the irreducible list.

| Unique trait | Why it works | Our version |
|---|---|---|
| Exposed mechanism as aesthetic | Turns engineering into decoration without adding decoration | Show generator state, conflict logic, deducibility |
| Light and dots as information | Communicates without demanding attention | Dot matrix as readout, never as texture |
| Dot-matrix type as brand voice | Departure-board reference — information you glance at | Doto for hero numerals only, 36sp+ |
| Instrument labelling | All-caps tracked mono labels make a screen a control surface | Geist Mono labels, 11–12sp, 0.06–0.1em |
| Restraint as confidence | No accent needed; hierarchy from type and space alone | Zero accent by default |
| Parenthetical lowercase naming | `phone(2a)`, `ear(open)` — a verbal signature, instantly theirs | `sudoku(9)`, `nonogram(10)` |

That last one is free and very high signal. Naming the games `sudoku(9)` and `minesweeper(10)` in
their own convention costs nothing and reads as authentically Nothing to anyone who knows the brand.

---

## 7. Do and don't

### Do

- Let type and whitespace carry the entire hierarchy
- Label things in words: all-caps, tracked, monospace
- Make every number a readout — tabular figures, monospace, aligned
- Give dots a job
- Expose state rather than concealing it
- Design the loading and empty states as carefully as the main screen
- Use haptics as the feedback channel, since colour and motion are constrained
- Keep one purpose per screen
- Align to a strict grid, then adjust optically where the grid lies

### Don't

- Don't add a decorative dot grid — it is the clearest tell of a copy
- Don't reach for red because a screen looks plain. Plain is correct.
- Don't use colour as decoration. Nothing's palette is functional or absent.
- Don't animate position, scale, or bounce. Motion is opacity and colour, fast and calm.
- Don't use icons where a word fits
- Don't let numerals shift width as they count
- Don't mistake sparse for unfinished. Empty space is the design.
- Don't ship their proprietary typefaces. See the licensing warning in `resource-map.md`.

---

## 8. How to verify fidelity, rather than guess at it

Our canon descends from a third-party interpretation, so our accuracy is capped by theirs. That is how
the font error got in. Raise the ceiling with primary sources:

1. **Build a reference board.** Screenshots of Nothing's Weather, Clock, Recorder, and Gallery beside
   our screens at matched scale. Compare tracking, weight, spacing rhythm, and optical alignment
   directly. This teaches more than any token table.
2. **Install their Icon Pack** (`com.nothing.icon`) and study stroke weight and grid discipline.
3. **Read their own developer materials.** The
   [Glyph Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) and
   [Glyph Matrix kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) are
   first-party and show how they think about the dot grid as an information surface.
4. **Watch nothing.community.** Staff participate; design intent surfaces there before press coverage.

---

## 9. Open decisions this study raises

1. **Which Nothing era do we target?** Austere 2022–23 flat monochrome, or current 5.0 translucent and
   tinted? Recommendation: austere, stated explicitly.
2. **Should red be "at most one" or "normally zero"?** Recommendation: normally zero. This changes
   Requirement 14's framing.
3. **Do we relax "no sharp corners"?** Nothing's own web CTA is a rectangle. Recommendation: keep our
   stricter rule, since it suits a grid game, but record that it is our choice, not their law.
4. **Do we adopt parenthetical lowercase naming?** Recommendation: yes. Free authenticity.
5. **Do we build the Glyph integration?** It is the strongest possible authenticity signal, but needs
   a permission carve-out and limits us to Nothing devices on Android 14+. Recommendation: optional
   product flavour, after tier 1 ships.

---

## 10. Designers on what makes Nothing work

### Dieter Rams — the lineage Nothing is claiming

Rams ran design at Braun from 1961 to 1995 and wrote the ten principles that Apple, Muji and now
Nothing all descend from. Two of his statements matter more than the famous "less, but better":

On detail: [*"Nothing works without details. They are everything, the baseline of quality."*](https://ifdesign.com/en/if-magazine/dieter-rams-10-principles-for-good-design)
Precision is not a garnish on minimalism — it is the entire substance. A sparse screen with sloppy
tracking is not minimal, it is empty. This is why our em-versus-sp tracking bug mattered so much: at
this level of restraint, the details *are* the design.

On economy: Braun's approach [leaves away everything superfluous to emphasise what is more important](https://interiordesign.net/designwire/how-dieter-rams-transformed-the-design-landscape-with-braun-s-less-is-more-ethos/).
Note the structure — removal is in service of *emphasis*. You subtract to make one thing louder. This
is exactly the argument for zero accent colour by default: strip everything so the single red conflict
indicator lands like a gunshot.

On being avant-garde, from a 1980 Braun board speech: good designers must
[question everything generally thought obvious](https://www.stirworld.com/inspire-people-dieter-rams-celebrating-the-genius-with-10-products-for-10-commandments-of-design).
Nothing's whole existence is that sentence applied to phones.

**Applied to us:** Rams gives permission to be austere, but demands the austerity be *exact*. Ten
suppression annotations, tabular figures, tracking in em — that pedantry is the Rams inheritance, not
bureaucracy.

### Jesper Kouthoofd — and the fact that changes how you should read Nothing

Kouthoofd founded Teenage Engineering and co-founded Nothing. He is CEO *and* head of design, and says
he is still [at the drawing board with the design team](https://www.peterzimon.com/blog/jesper-kouthoofd-interview-2024/)
rather than running daily operations — design authority sits at the top, not in a service function.

His stated attitude: [stay curious, stay naïve, try new things](https://www.sfmoma.org/read/stay-curious-stay-naive-an-interview-with-teenage-engineering-jesper-kouthoofd/).
Naïveté as a method, not a flaw.

**And the detail that should reframe this entire project:** Teenage Engineering was founded by
[game designers](https://icon.jp/archives/26752) — Kouthoofd, David Eriksson, Jens Rudberg and David
Möllerstedt came out of games before building the OP-1.

That is not trivia. The aesthetic you are borrowing was built by people who made games, then applied
game-design instincts to instruments: immediate feedback, playful constraint, controls that reward
learning, a device you want to touch. Building a *game* in Nothing's language is not a stretch of the
brand. It is closer to the source than their phones are.

Follow the implication: an OP-1 is fun to operate before you know what any button does. Your Sudoku
should feel good to poke at before the player understands the game. That is a Teenage Engineering
standard, not a Sudoku standard.

### Adam Bates — design the screen like a logo

Bates on their method: a phone should look and feel like an image, a graphic design,
[*"almost like a logo"*](https://www.creativebloq.com/design/product-design/why-nothing-designs-its-phones-like-logos),
which is why each product carries the instant recognition of a good logo.

**Applied to us:** the Sudoku screen should be recognisable as a silhouette. Squint until the detail
blurs — is the composition still distinctly ours? If it collapses into "generic dark grid app," the
layout has failed regardless of token compliance. This is a better review test than any checker: blur
the screenshot and see if it still has an identity.

---

## 11. The strongest criticism, and why it improves our plan

Taking the critics seriously is how we avoid the shallow version.

**iFixit's charge is the sharpest.** They found Phone (1)'s transparency
[deceptive](https://www.ifixit.com/News/64265/nothings-phone-1-isnt-so-transparent-after-all) — the
attention-grabbing complexity actually makes the phone *harder* to repair. You can see the internals,
but seeing them does not help you.

That lands. And it forces a real correction to section 1 of this study.

Nothing does not expose the mechanism. Nothing exposes a **composed, curated view** that *reads as*
exposed mechanism. Bates said as much: a flexible PCB is not beautiful by default, and they had to
work to make it look that way. What you see through the glass is art-directed, not honest.

**This is a critical correction for us.** "Expose the mechanism" does not license dumping raw state
into the UI. A debug overlay of generator internals is not Nothing, it is the opposite — it is
undesigned. The instruction is:

> Compose a small, deliberate set of readouts that make the machine *legible*. Choose what to reveal.
> Design the reveal as carefully as the thing revealed.

Three readouts chosen well beat twelve dumped honestly.

**Other criticism worth holding.** The Glyph Matrix is widely called a gimmick, and The Verge's review
is [pointedly dry about it](https://www.theverge.com/mobile/709093/nothing-phone-3-review). Phone (4a)
Pro [dropped most of the transparency](https://9to5google.com/2026/03/05/nothing-phone-4a-pro-hands-on/)
that defined the brand. Transparency also made dust and moisture ingress
[highly visible](https://android.gadgethacks.com/news/nothing-phones-trust-problem-premium-price-basic-flaws/),
turning the signature into a liability.

**The lesson for a game:** a distinctive feature that does not do work gets called a gimmick, fast. A
dot-matrix background that is purely decorative is our gimmick risk. Give the dots a job and the
criticism has nowhere to land. This is the same conclusion section 3 reached from the opposite
direction, which is a good sign.

---

## 12. Four review tests, in order of usefulness

Cheap, fast, and they catch what a conformance checker cannot.

1. **The squint test.** Blur the screenshot until detail disappears. Is the composition still
   identifiably ours, or is it a generic dark grid? (Bates — design it like a logo.)
2. **The grayscale test.** Desaturate. Is every error state still readable? (Accessibility, and it
   proves red is not load-bearing.)
3. **The gimmick test.** Point at every distinctive element and name the job it does. Anything with no
   answer is decoration — remove it. (iFixit, the Glyph critics.)
4. **The detail test.** Zoom to 400% on labels and numerals. Is tracking correct, are figures tabular,
   is the baseline grid honoured? (Rams — nothing works without details.)
