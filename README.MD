---
marp: true
style: "
      :root
      {
        --color-canvas-default: rgb(20,20,20);
        --header-color: rgb(130, 90, 255);
      }

      h1
      {
        color: var(--header-color);
        font-size: 3em;
      }

      *
      {
        color: rgb(210, 220, 255);
      }

      ul
      {
        list-style: none;
      }

      section.title-page *
      {
        text-align: left;
        margin: 0;
      }

      section.title-page h2
      {
        font-size: 1.5em;
        font-weight: normal;
      }

      section.title-page h3
      {
        font-size: 0.75em;
        font-weight: lighter;
      }

      section:not(.title-page) *:is(h2, h3, h4, h5, h6)
      {
        text-align: center;
        color: var(--header-color);
      }

      h2
      {
        font-size: 2em;
      }

      h3
      {
        font-size: 1.6em;
      }

      section.double-title h3
      {
        margin-bottom: 1em;
      }

      section.double-column>ul,
      section.double-column>ol
      {
        column-count: 2
      }

      a
      {
        color: rgb(190, 150, 255);
      }
       "
---
<!-- _class: title-page -->

# Regular Expressions to HLS

## Status as of the 9th of October

### Pedro Miguel Ribeiro Alves - <https://github.com/specs-feup/hls-regex-engine>

---

<!-- paginate: true -->
<!-- _class: double-column -->

## Rulesets

Extracted **67996** regular expressions from:

1. Protomata (From [AutomataZoo](https://github.com/tjt7a/AutomataZoo))
2. [ClamAV](https://database.clamav.net) 'Main' Database
3. [Proofpoint's Suricata 4.0 Rules](https://rules.emergingthreats.net/open/suricata-4.0/)
4. [Proofpoint's Suricata 5.0 Rules](https://rules.emergingthreats.net/open/suricata-5.0/)
5. [Snort 2.9](https://rules.emergingthreats.net/open/snort-2.9.0/)
6. [Snort3 Community Rules](https://www.snort.org/downloads)
7. [Yara](https://github.com/Yara-Rules/rules)

## Ruleset Characteristics

- ✅ Feature Count
- ✅ Flag Count
- ✅ Backreference Length
- ✅ Expression Length

---

<!-- _class: double-column double-title -->

## Regular Expression Features

### Planning to Implement

- ✅ Character Classes (41520)
- ✅ Concatenations (3356365)
- ✅ Alternations (17367)
- ✅ Start/End Anchors (8546/6731)
- ✅ Regular Quantifiers `*`, `+`, `?` (22098)
- ✅ Bounded Quantifiers* (10657)
- ✅ Capture/Non-Capture Groups (21981/8880)
- ✅ Backreferences (335)
- ✅ Flags (20517)
  - ✅ Case Insensitive Match `i` (7045)
  - ✅ Multi Line Match `m` (3202)
  - ✅ Single Line Match `s` (1340)
  - ✅ Anchored `A` (2)
  - ✅ Extended `x` (0)

---

### Not Planning to Implement

- Lookarounds (700)
- Lazy/Possessive Quantifiers (5381/11)
- Flags
  - Ungreedy quantifiers as default `U` (?)
  - Allow duplicate subpattern names `J` (0)
  - Platform Exclusive Flags
    - [Suricata:](https://redmine.openinfosecfoundation.org/projects/suricata/wiki/Pcre_(Perl_Compatible_Regular_Expressions)) `R`, `U`, `I`, `P`, `Q`, `H`, `D`, `M`, `C`, `S`, `Y`, `B`, `O`, `V`, `W`
    - [Snort:](http://manual-snort-org.s3-website-us-east-1.amazonaws.com/node32.html#SECTION004526000000000000000) `R`, `U`, `I`, `P`, `H`, `D`, `M`, `C`, `K`, `S`, `Y`, `B`, `O`

---

## Optimizations

- ✅ NFA to DFA conversion
- ❌ Affix identification and reutilization
- ❌ Character Decoder
