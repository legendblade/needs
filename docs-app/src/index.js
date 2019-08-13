import React from 'react';
import ReactDOM from 'react-dom';
import ReactMarkdown from 'react-markdown';
import ScrollableAnchor from 'react-scrollable-anchor'
import { cloneDeep, clone, startCase, snakeCase, union, unionBy, map, sortBy, differenceBy, isString } from 'lodash';
import { Light as SyntaxHighlighter } from 'react-syntax-highlighter';
import js from 'react-syntax-highlighter/dist/esm/languages/hljs/javascript';
import vs from 'react-syntax-highlighter/dist/esm/styles/hljs/vs';
 
SyntaxHighlighter.registerLanguage('javascript', js);

const noDesc = (<span class='text-muted'>There's no description of this item; encourage the mod author to add one, either through the API or localization.</span>);

function sortEntries(entries) {
    return sortBy(entries, 'id');
}

function NavBar(props) {
    if (!props.value.children || props.value.children.length <= 0) {
        return (
            <a class="nav-link"
                href={props.value.root}>
                    {props.value.title}
            </a>
        );
    }

    const children = props.value.children.map((c) => {
        return (<NavBar key={c.root} value={c} />);
    });

    return (
        <nav class="navbar navbar-light">
            <a class={props.value.class || "nav-link"}
                href={props.value.root}>
                    {props.value.title}
            </a>
            <nav class="nav nav-pills flex-column">
                {children}
            </nav>
        </nav>
    );
}

function AliasList(props) {
    if (props.aliases && 0 < props.aliases.length) {
        return (
            <div class='mt-2'>
                This can be created through the following type names:
                <ul>
                    {props.aliases.map((a) => (<li key={a}>{a}</li>))}
                </ul>
            </div>
        );
    } else {
        return (<div><small class='text-muted'>This cannot be directly added</small></div>);
    }
}

function Field(props) {
    props.depth = props.depth || 0;

    const rowClass = 'row mx-1 ' + ((props.isParent) ? 'text-muted' : '');
    const nameClass = props.isParent ? 'text-muted' : '';
    const typeBadge = 'badge float-left mr-1 ' + ((props.isParent) ? 'badge-light' : (props.type === 'Variable' ? 'badge-primary' : 'badge-info'));
    const expBadge = 'badge float-left ' + ((props.isParent) ? 'badge-secondary' : 'badge-success');
    const parentText = props.isParent ? (<span key="1" class='text-muted'><small>From {props.parent}</small></span>) : '';
    const showMap = props.isMap && props.type !== "Map";
    const showArray = props.isArray && props.type !== "Array";
    
    const prefix = showMap 
                        ? "Map of Strings->"
                        : showArray
                            ? "Array of "
                            : "";
    const suffix = showArray ? "s" : ""

    const hasSubfield = props.listOrMapClass && (props.listOrMapClass.description || (props.listOrMapClass.fields && props.listOrMapClass.fields.length));

    let d = (
        <div class='p-2'>
            <span class={typeBadge}>{prefix}{startCase(props.type)}{suffix}</span>
            {props.isExpression ? (<span class={expBadge}>Expression</span>) : ('')}
            <div class={nameClass}>
                {props.name}
            </div>
            {props.isOptional ? (<div class='text-muted'><small>Optional; defaults to: {props.defaultValue}</small></div>) : ''}
            {parentText}
        </div>
    )

    const thStyle = {
        'borderLeft': '1px solid #CCC',
        'display': 'block',
        'height': '100%',
        'marginLeft': '1rem',
        'paddingLeft': '1rem'
    };
    for (let i = 0; i < props.depth; i++) {
        d = (<div style={thStyle}>{d}</div>);
    }

    const field = (
        <tr key={props.name} class={rowClass}>
            <th class='col-md-4 text-right p-0'>
                {d}
            </th>
            <td class='col-md-8 p-2'>                
                <ReactMarkdown source={props.description || noDesc} linkTarget='_blank' />
                {props.isExpression ? (<div class='mt-4'><span class='badge badge-success'>Expression</span> The following variables are available for this expression</div>) : ""}
                {hasSubfield ? (<div class='mt-4'><strong>{startCase(props.type)}</strong>: <ReactMarkdown source={props.listOrMapClass.description || noDesc} linkTarget='_blank' /></div>) : ""}
            </td>
        </tr>
    );

    if (!hasSubfield && !props.isExpression) return field;

    if (hasSubfield) {
        const hasVisited = {};
        return [field,map(props.listOrMapClass.fields, (f) => {
            f.isParent = props.isParent;
            f.parent = props.parent;
            f.depth = props.depth + 1;

            if (hasVisited[f.type]) delete f.listOrMapClass;
            hasVisited[f.type] = true;
            return Field(f);
        })];
    }

    return [field,map(props.expressionVars, (v, k) => {
        return Field({
            'name': k,
            'description': v,
            'type': 'Variable',
            'isParent': props.isParent,
            'depth': props.depth + 1,
            'parent': props.parent,
            'expressionVar': true
        });
    })];

}

function FieldList(props) {
    if (!props || props.length <= 0) return ('');
    return (
        <table class='table table-striped table-borderless'>
            <tbody>
                {props.map((f) => Field(f))}
            </tbody>
        </table>
    );
}

function Entry(props, parentFields) {
    props.depth = props.depth || [];
    const name = startCase(props.id);

    const hLvl = (props.depth.length < 4) ? props.depth.length + 3 : 6;
    const Tag = 'h' + hLvl;
    const container =  {
        'marginLeft': (props.depth.length * 20 + 'px')
    };

    const modColor = (function(str) {
        // Adapted from https://stackoverflow.com/a/16348977
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            hash = str.charCodeAt(i) + ((hash << 5) - hash);
        }

        let colour = '#';
        for (let i = 0; i < 3; i++) {
            const value = ((hash >> (i * 8)) & 0x88)// + 0x77;
            colour += ('00' + value.toString(16)).substr(-2);
        }
        return colour;
    })(props.mod || "");

    const output = [(
        <ScrollableAnchor  id={props.href}>
            <div class='card mt-4 p-2' style={container}>
                <div class='row'>
                    <div class='col-9'>
                        <Tag class='card-title'>{name}<small>{props.depth.map((d) => (<a class='entry-path' href={'#' + d.href}>{d.name}</a>)).reverse()}</small></Tag>
                    </div>
                    <div class='col-3 text-right'>
                        {
                            props.aliases.length 
                                ? (<div class="badge badge-pill text-white" style={{"backgroundColor": modColor}}>{props.mod || "Unknown"}</div>) 
                                : (<div class="badge badge-pill badge-dark">Abstract</div>)
                        }
                    </div>
                </div>
                <ReactMarkdown source={props.description || noDesc} linkTarget='_blank' />

                <AliasList aliases={props.aliases} />
                {FieldList(union(parentFields, props.fields))}
            </div>
        </ScrollableAnchor>
    )];

    const pfields = unionBy(parentFields || [], cloneDeep(props.fields).map((f) => {
                        f.isParent = true;
                        f.parent = name;
                        return f;
                    }), 'name'); // Called this way to ensure ordering

    const newDepth = union(clone(props.depth), [{'name': name, 'href': props.href}]);
    sortEntries(props.children).forEach((c) => {
        c.depth = newDepth;
        output.push(Entry(c, differenceBy(pfields, c.fields, 'name')))
    });

    return output;
}

function TutorialCode(code) {
    return (
        <SyntaxHighlighter language="javascript" style={vs}>
            {JSON.stringify(code, null, 2)}
        </SyntaxHighlighter>
    );
}

class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            'navroot': {},
            'localData': '###LOCALDATA###'
        };
        this.getData();
    }

    parseData(response) {
        console.log(response);
        const convertToNav = (v, r) => {
            v.href = snakeCase(r + '_' + v.id);
            return {
                "title": startCase(v.id),
                "root": "#" + v.href,
                "children": map(sortEntries(v.children), (c) => convertToNav(c, r))
            };
        };
        
        const navroot = {
            "title": "Needs, Wants, and Desires",
            "root": "#",
            "class": "navbar-brand d-block text-wrap",
            "children": [
                { "title": "Needs", "root": "#needs" },
                { "title": "Need Types", "root": "#needTypes", "children": map(sortEntries(response.needs), (c) => convertToNav(c, "needTypes")) },
                { "title": "Mixins", "root": "#mixins", "children": map(sortEntries(response.mixins), (c) => convertToNav(c, "mixins")) },
                { "title": "Manipulators", "root": "#manipulators", "children": map(sortEntries(response.manipulators), (c) => convertToNav(c, "manipulators")) },
                { "title": "Levels", "root": "#levels" },
                { "title": "Actions", "root": "#actions", "children": map(sortEntries(response.actions), (c) => convertToNav(c, "actions")) }
            ]
        };

        this.setState({
            'navroot': navroot,
            'needs': sortEntries(response.needs.map((n) => (Entry(n)))),
            'mixins': sortEntries(response.mixins.map((n) => (Entry(n)))),
            'manipulators': sortEntries(response.manipulators.map((n) => (Entry(n)))),
            'actions': sortEntries(response.actions.map((n) => (Entry(n))))
        });
    }

    getData() {
        if (!isString(this.state.localData)) {
            new Promise((resolve) => resolve(this.state.localData))
                .then((response) => this.parseData(response));
            return;
        }

        fetch('./data.json')
            .then((response) => {
                return response.json();
            })
            .then((response) => this.parseData(response));
    }

    render() {
        return (
            <div class="row">
                <div class="col-xl-3 col-lg-4 d-none d-lg-block">
                    <nav id="scrollnav" class="navbar navbar-light">
                        <NavBar value={this.state.navroot} />
                    </nav>
                </div>

                <div class="col-12 col-lg-8 px-4" style={{'border-left': '1px solid #DEDEDE'}}>
                    <div>
                        <h2>Overview</h2>
                        <p class="lead">
                            There are a couple of major topics that you will need to know when creating needs.
                        </p>
                        <dl class="row">
                            <dt class="col-sm-2">Need Types</dt>
                            <dl class="col-sm-10">
                                <p>
                                    Needs can be custom - meaning that it's brand new functionality being added - or they can be associated with an already-existing
                                    stat - some examples which are included by default are a player's max health, or the amount of sunlight they're currently receiving.
                                </p>
                                <p>
                                    Each specific type of need may have specific options associated with it, or may be read-only. Check the relevant section to see
                                    information on each.
                                </p>
                            </dl>
                            <dt class="col-sm-2">Mixins</dt>
                            <dl class="col-sm-10">
                                <p>
                                    Needs by themselves are generally hidden, background things that the player can't see. Mixins can help resolve that by adding
                                    specific functionality, such as notifying the user with a chat message when they change levels, or displaying information in
                                    the UI.
                                </p>
                                <p>
                                    Mixins are in a broad sense functionality that can be shared across needs and mixed-in when you want to use that functionality.
                                </p>
                            </dl>
                            <dt class="col-sm-2">Manipulators</dt>
                            <dl class="col-sm-10">
                                <p>
                                    While needs that are based on existing stats - such as health, food, and breath - and are changed by outside forces, your custom
                                    needs will need to know how and when to change. Manipulators can provide that functionality by allowing you to define what affects
                                    a particular need and how.
                                </p>
                                <p>
                                    However, manipulators don't only apply to custom needs - you can also apply them to any need that isn't read-only; say you wanted
                                    to make the player regenerate health when they first enter sunlight - just use the <code>onNeedChanged</code> manipulator to link up
                                    the <code>sunlight</code> need with the <code>health</code> need. But maybe that's too nice, and instead you want to... convince the
                                    player to be more passive, you could use the <code>holding</code> manipulator to say that anytime they're holding a sword or axe, you
                                    want to decrease their <code>health</code> by a certain amount - you can do that too.
                                </p>
                            </dl>
                            <dt class="col-sm-2">Levels</dt>
                            <dl class="col-sm-10">
                                <p>
                                    Okay, so you have a need, and that's cool, but you want to affect the player in certain ways when they're at a certain level - take
                                    the idea that spawned this mod: nutritition (or, specifically: Nutrtition, the mod). You want to apply buffs when the player is at
                                    high nutritional values, and apply debuffs when they're at low values. That's where levels come into play.
                                </p>
                                <p>
                                    And if you associate the <code>ui</code> mixin, you even get nice looking bars showing you where a particular level starts and ends:
                                </p>
                                <div class="text-center">
                                    <img src="https://pbs.twimg.com/media/EA6TYfiUYAAOyXX?format=png&name=small" alt="UI with bar" class="rounded" />
                                </div>
                                <p>
                                    Like mixins and manipulators - levels aren't only for your own custom needs - you can assign levels to <i>any</i> need, including
                                    read-only needs. So go forth, don't let your dreams be dreams, add some levels to the <code>moonPhase</code> need, and make the
                                    player a wereparrot during the full mooon.
                                </p>
                            </dl>
                            <dt class="col-sm-2">Actions</dt>
                            <dl class="col-sm-10">
                                <p>
                                    Levels by themselves are cool, right? I mean, look at those slick UI bars. Okay, but maybe you want the levels to <i>do</i> something
                                    beyond just looking pretty - that's where actions come into play.
                                </p>
                                <p>
                                    Each action defines something that happens - either once, or continuously while the player is at the level the action is assigned to - 
                                    in order to breathe some life into your levels - possibly literally, you can use the <code>adjustNeed</code> action to target the
                                    player's <code>breath</code> need.
                                </p>
                            </dl>
                            <dt class="col-sm-2">Expressions</dt>
                            <dl class="col-sm-10">
                                <p>
                                    These aren't going to get a separate section by themselves, because they'll be covered inline each time they show up - expressions
                                    are a way to make things more dynamic. On a basic level, most manipulators, rather than just taking in a value that they adjust
                                    their associated need by, can instead take a mathematical expression like this:
                                </p>
                                <p class="text-center">
                                    <code class="text-center">current / 2</code>
                                </p>
                                <p>
                                    ... or this:
                                </p>
                                <p class="text-center">
                                    <code>min(current,4) + max(-2,need(Some Other Need))</code>
                                </p>
                                <p>
                                    Or anything else you can find over at <a href="http://mathparser.org/mxparser-math-collection/">http://mathparser.org/mxparser-math-collection/</a>
                                    in order to let you have full control over how things work.
                                </p>
                                <p>
                                    Do note that each individual expression may have different variables you can use - be sure to read the description of each to check
                                    what it supports. When an expression is available, it'll be marked like this: <span class='badge badge-success'>Expression</span>
                                </p>
                                <p>
                                    In addition to any functions from the base mXparser library, there's a special function <code>need(Name)</code> <small class='text-muted'>(okay,
                                    technically it's actually a variable - internally the mod will remap need(Name) to 'needA', 'needB', etc in order to parse the expression)
                                    </small>. When used in your expressions, you can retrieve the current value of any other named need.
                                </p>
                                <p>
                                    One last thing - you don't <i>have</i> to use an expression if you don't want to. And you don't necessarily have to use any of the variables
                                    available to you. When an expression is being parsed during load, it'll go through the following checks, in order:
                                </p>
                                <ul>
                                    <li><b>Is the value a number?</b> - That number will always be used, and no additional overhead will happen during gameplay</li>
                                    <li>
                                        <b>Is the value an expression that doesn't use any variables?</b> - 
                                        The loader will run the expression, and use the result; this happens during load, however, and, as above, no additional overhead will
                                        happen during gameplay. You happen to forget what 2 + 5 is? Feel free to put "2 + 5" as your expression; we've got you.
                                    </li>
                                    <li>
                                        <b>Does the expression use one or more variables?</b> - 
                                        The loader will figure out which variables you're using, and only ever parse those during gameplay; this will have a gameplay performance
                                        impact compared to the above two cases, but we're talking nanoseconds. Unless you have hundreds of needs, or the variable itself
                                        takes excessively long to calculate, you shouldn't worry too much about it.
                                    </li>
                                    <li>
                                        <b>Does the expression use one or more linked needs?</b> -
                                        If using another need in your expression, the loader will parse these out like variables above. Do note that this will incur an even
                                        higher performance impact than a normal variable, but it should still be negligible in almost all cases.
                                    </li>
                                </ul>
                            </dl>
                        </dl>
                        <p>
                            With all that out of the way, let's dive in to the various sections to see what all can be accomplished.
                        </p>
                        <div class="alert alert-light">
                            Note, if you're reading this document from the mod's config directory, it's been automatically generated from the mods you have installed - 
                            so if any mod adds an extra need, mixin, manipulator, or level action, it'll show up here after the first time you run the game with that mod installed.
                        </div>
                    </div>
                    <hr />
                    <div id="needs">
                        <h2>Needs</h2>
                        <p>
                            In order to get started creating needs, you'll want to fire up a text editor (or preferably an IDE like VSCode), and navigate
                            to <code>&lt;Minecraft&gt;/config/needs</code>. This directory will be where all of your needs get configured (go figure, right?).
                        </p>
                        <p>
                            The mod will read any JSON file placed in this directory in order to create a need. Let's start out a basic JSON file now; in your editor,
                            create a new file called <code>tutorial.json</code>, and start off by putting in the name of the need:
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom"
                        })}
                        <p>
                            If we started the game now, the system would try and load this need (it might even succeed, but probably not), but because we're missing
                            a few bits of information, it won't do much of anything. Let's define a few more properties now - specifically <code>min</code>, <code>max</code>, 
                            and <code>initial</code>
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15
                        })}
                        <p>
                            Okay, with that, we've specified that our new custom need will have a range of 0-100, and will start at 15. 15 what, you ask? Well, that's entirely
                            up to you. For custom needs, the numbers used - much like every other point system - don't really matter; we could have just as easily specified
                            that the min was -500, and the max was 8675309, and our example would still work. We'll provide context to our players later.
                        </p>
                        <p>
                            Whatever your choice of range, at the moment, the need is completely static - nothing manipulates (affects, changes, don't ask why I chose "manipulators"
                            as the term; there's probably some psychological reason, but I don't want to think about that right now...) our new need. Let's set up a way to
                            increase the value of the need now.
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [
                                {
                                    "type": "lookingAt",
                                    "amount": 0.05,
                                    "blocks": [
                                        "minecraft:bookshelf"
                                    ]
                                }
                            ]
                        })}
                        <p>
                            Here, we're declaring the array of manipulators - this is always an array, even if you only have one item in it - and adding the <code>lookingAt</code> manipulator
                            to it (for an overview of all available manipulators, look at the 'Manipulators' section, below). The <code>lookingAt</code> manipulator
                            does exactly what you'd expect - anytime you're looking at one of the blocks listed, it triggers, and adjusts the need by the amount provided. Man,
                            I wish I could gain knowledge just by staring at a bookshelf.
                        </p>
                        <div class="alert alert-dark mx-3">
                            <p>
                                I'm going to call this out separately here, because it's important - many needs and manipulators that involve the world around the player will do so
                                by periodically scanning at set intervals in a tick handler. It's not necessary to understand exactly how all of this works, but, generally keep in
                                mind that:
                            </p>
                            <ul>
                                <li>
                                    <b>Only needs and manipulators that are used somewhere will get registered.</b> I've tried to optimize tick handling to be as light as possible
                                    while still enabling mod makers, pack makers, and players to create in-depth stat systems.
                                </li>
                                <li>
                                    <b>Anything that "ticks" does so every 5 ingame ticks, or roughly 1/4th of a second.</b> This has a couple of reasons for needing stated.
                                    <ul>
                                        <li>
                                            First, in our example above, if you'd specified, say '1' as the amount, your need would have increased by 4 every second,
                                            so, looking at that bookshelf would very, <i>very</i> quickly fill up the need.
                                        </li>
                                        <li>
                                            Secondly, this does not mean that every player is ticked at the same time. When the mod starts, it creates 5 buckets. As players
                                            log in, they get assigned to one of the buckets with the fewest number of other players. Every tick, the mod will loop through
                                            one of the 5 buckets, in sequential order, to do the relevant work for the players in that bucket. This helps mitigate the
                                            overhead of calling all the players all at once.
                                        </li>
                                    </ul>
                                </li>
                                <li>
                                    <b>Be mindful of performance, but don't let paranoia hold you back.</b> As mentioned above, I've tried very heavily to optimize
                                    much of the code; create your systems, and <i>then</i> see if it's really a problem. I hope it never will be.
                                </li>
                            </ul>
                        </div>
                        <p>
                            Alright, with that heavy note out of the way, let's lighten up again. We have a way to increase our need, so let's add some ways to decrease it.
                            What would decrease one's knowledge? How about eating rotten flesh? But let's also add a bit if the player drinks some milk - why milk? It does
                            a body good.
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [
                                {
                                    "type": "lookingAt",
                                    "amount": 0.05,
                                    "blocks": [
                                        "minecraft:bookshelf"
                                    ]
                                },
                                {
                                    "type": "itemUsed",
                                    "defaultAmount": 5,
                                    "showTooltip": true,
                                    "items": {
                                        "milk_bucket": null,
                                        "rotten_flesh": "-current/2"
                                    }
                                }
                            ]
                        })}
                        <p>
                            Okay, okay, I know, it's getting complicated, but, stick with me and let's break it on down. We have a new manipulator - this one is 
                            an <code>itemUsed</code> manipulator - that will trigger anytime the player finishes using an item (in our case, eating/drinking). Now,
                            we specify a default amount - there's multiple reasons why this would be used, and that's because the format for the item list is very
                            forgiving - for now, just know that in our example, the <code>defaultAmount</code> will be used for the milk bucket.
                        </p>
                        <div class="alert alert-dark mx-3">
                            <p>Let's take a very brief aside to talk item formats. The following are all valid:</p>
                            <ul>
                                <li><b>milk_bucket</b>: This will refer to the Minecraft 'milk_bucket' item.</li>
                                <li>
                                    <b>minecraft:milk_bucket</b>: Same as above, but we're specifying that it's specificaly from 'minecraft'; if you have a modded
                                    item, you will have to specify the name of the mod here.
                                </li>
                                <li>
                                    <b>tag:forge:ingots/iron</b>: Tags are a new 1.14 concept. In the ancient beforetimes, we had ore dictionaries. Now, we're all
                                    cool and hip with our 'tags'. If you want to specify a bunch of items, but don't know what they are, that's a tag. In this case,
                                    we're specifying the <code>tag</code> from <code>forge</code> for <code>ingots</code> made out of <code>iron</code>.
                                </li>
                            </ul>
                        </div>
                        <p>
                            We're going to skip over <code>showTooltip</code> for the moment, and go straight to our item list, which is another object - this time
                            we're specifiying it as a key/value pair of <code>item or tag: the value to use</code>. Our first item, the <code>milk_bucket</code>,
                            is set to null - in this form of the list, anything that's set to null will default back to the <code>defaultAmount</code>. You might
                            be able to save yourself some typing there? (don't worry, there's another format we'll discuss after this where it makes more sense)
                        </p>
                        <p>
                            Now, onto our second item, the <code>rotten_flesh</code> - what's this? That's not a number! Correct. As mentioned in the overview (you did
                            read the overview, right?), most places that accept a number (and it makes even the slightest amount of sense) will also accept an expression.
                            Which is scary math nonsense, right? Well, in this case, we're saying that "anytime you eat rotten flesh (ick), decrease the need by half".
                        </p>
                        <p>
                            If you were to load up the game now, and moused over a piece of rotten flesh in your inventory, it'd look something like this:
                        </p>
                        <div class="text-center">
                            <img src="https://pbs.twimg.com/media/EAqgNX2UYAEeS8h?format=png&name=360x360" alt="Rotten Flesh" class="rounded" />
                        </div>
                        <p>
                            ... why does it say Protein and not Learning? I'm not recycling screenshots, you are.
                        </p>
                        <p>
                            Ahem. Anyway. With the default tooltip handling, anything that's a positive number will be green, and anything that's a negative number
                            will be red in the tooltip. Green you say? But it's a milk bucket. Shouldn't that be white? Say no more.
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [
                                {
                                    "type": "lookingAt",
                                    "amount": 0.05,
                                    "blocks": [
                                        "minecraft:bookshelf"
                                    ]
                                },
                                {
                                    "type": "itemUsed",
                                    "defaultAmount": 5,
                                    "showTooltip": true,
                                    "items": {
                                        "milk_bucket": null,
                                        "rotten_flesh": "-current/2"
                                    },
                                    "formatting": {
                                        "white": "(0,10]"
                                    }
                                }
                            ]
                        })}
                        <p>
                            Well that's a bit ugly, but it gets the job done, right? The new <code>formatting</code> object is another key/value map, this time
                            with Minecraft text formatting codes (which are available over 
                            at <a href="https://minecraft.gamepedia.com/Formatting_codes#Color_codes">https://minecraft.gamepedia.com/Formatting_codes#Color_codes</a>)
                            acting as the key, and a range or interval specified as the value (in this case, from 0, inclusive, to 10, exclusive - which is really a way of saying
                            that it's going to be any number, starting at and including 0 and going up to 10). I'm going to leave a link here to Wikipedia on
                            it: <a href="https://en.wikipedia.org/wiki/Interval_(mathematics)#Notations_for_intervals">Wikipedia: Interval (mathematics)</a>, so 
                            read up there if you're a bit rusty on the syntax. Or don't. I tried to make the ranges as open as possible.
                        </p>
                        <p>
                            Alright. One more example of manipulators before we move on. If only because I want to show off the expression system more. Let's penalize
                            the player for dying. Because <i>of course</i> we penalize the player for dying. Git gud scrub. This time, I'm just going to show the new
                            manipulator, because you already know to just put a comma and add it to the end of the array, right?
                        </p>
                        {TutorialCode({
                            "type": "onDeath",
                            "amount": "min(-current/4, -10)",
                            "downTo": 15
                        })}
                        <p>
                            Much the same as before, we have a new type, the <code>onDeath</code> manipulator. No surprise that this one triggers when the player dies.
                            We're also using another expression - this time, it's more complex - we take the minimum (<code>min</code>) of 1/4th the current value 
                            (<code>-current/4</code>) or <code>-10</code>. Now, this is a bit tricky, and I could have written this differently to make it less so,
                            but I wanted to <del>be complicated for no reason</del> make sure that y'all were paying attention. Because we're dealing with negative
                            numbers here, the minimum is actually the larger reduction - so, if the current value is 100, <code>100/4 = 25</code>, which, since -25 is
                            smaller than -10, the player will lose 25 points.
                        </p>
                        <p>
                            We could have also written that as <code>0-max(current/4, 10)</code> and it might have been a bit easier to understand what was going on.
                            Either way, the result is that the player will lose 1/4th of their points, but will always lose at least 10 points.
                        </p>
                        <p>
                            Oh, and one more thing. In case we wanted to avoid the player losing fractional points (which is a bit pointless because we're <i>adding</i>
                            fractional points in the <code>lookingAt</code> section), we can always <code>round(min(-current/4, -10), 0)</code>, which will round the
                            result to 0 decimal places.
                        </p>
                        <p class="alert alert-dark mx-3">
                            The underlying point here: You can get super complicated with expressions - especially because mXparser has <i>a lot</i> of functions for
                            you to use. But, I will go back to the note on performance - pick your battles - an expression that takes up three screens of text might
                            be fine for calculating when a player dies, but not so much if you're testing it on every tick. Again: don't be scared to experiment, 
                            but do keep performance in mind.
                        </p>
                        <p>
                            So, we have a need, and we have things that change the value of the need, how about we <i>do something with it</i>. Let's add some 
                            levels. <small>Note, I've cut out the manipulators, so this example doesn't get huge.</small>
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [],
                            "levels": [
                                {
                                    "name": "Genius",
                                    "min": 80
                                },
                                {
                                    "name": "Learned",
                                    "min": 50,
                                    "max": 80
                                },
                                {
                                    "name": "Dumb as Rocks",
                                    "max": 10
                                }
                            ]
                        })}
                        <p>
                            Now we have four levels. Wait, four levels, but we only declared three? Right - we have "Dumb as Rocks", from 0 (our minimum) to 10 (not including 10),
                            and "Learned" from 50 to 80 (not including 80), and "Genius" from 80 to our maximum - but we also have the gap between "Dumb as Rocks" and "Learned"
                            which automatically gets a level called "Neutral". Keep that in mind when designing your systems - if you have a spot that isn't covered by any other
                            level, the player will be "neutral" during that time.
                        </p>
                        <p class="alert alert-dark mx-3">
                            I'm contractually obligated to point out here that levels may not overlap, and that's why the maximum is exclusive, while the minimum is inclusive.
                            I plan to open up the ability in the future to specify it more precisely using interval notation, but we're not there yet. I also plan on allowing
                            you to share actions (we'll see those in a minute) between two levels, but we're also not there yet. We're somewhere though. Probably in the realm of
                            insanity. Who knows.
                        </p>
                        <p>
                            Great. We have levels. They don't do anything right now. But we have them. Let's spice things up, starting with "Learned":
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [],
                            "levels": [
                                {
                                    "name": "Genius",
                                    "min": 80
                                },
                                {
                                    "name": "Learned",
                                    "min": 50,
                                    "max": 80,
                                    "actions": [
                                        {
                                            "type": "potionEffect",
                                            "effect": "jump_boost"
                                        }
                                    ]
                                },
                                {
                                    "name": "Dumb as Rocks",
                                    "max": 10
                                }
                            ]
                        })}
                        <p>
                            That's it - just by specifying the <code>potionEffect</code> action, and telling it we want the <code>jump_boost</code> potion effect,
                            we'll let the player jump higher when they're in the "Learned" level. How? Uh... knowledge of physics and such. Let's you defy gravity.
                            Yep.
                        </p>
                        <p>
                            Okay, now let's dive in a bit more and do something with Genius.
                        </p>
                        {TutorialCode({
                            "name": "Genius",
                            "min": 80,
                            "actions": [
                                {
                                    "type": "potionEffect",
                                    "effect": "jump_boost",
                                    "amplifier": 2
                                },
                                {
                                    "type": "adjustNeed",
                                    "need": "attack",
                                    "amount": 5
                                }
                            ]
                        })}
                        <p>
                            So, we're doing two things here, firstly, we're just giving them jump boost again, but this time we're bumping it up to level 3. Big deal.
                            The other thing we're doing is adjusting another need with the <code>adjustNeed</code> - at this point, the player is so smart, they
                            know how to hit the right spots, and their <code>attack</code> deals an extra 5 damage. [Dire voice:] How cool is that?
                        </p>
                        <p>
                            While not shown here - because anything I put would be an even more contrived example (and that's saying a lot), there are two other
                            ways you can specify actions to run - <code>onEnter</code> and <code>onExit</code>. These will be called when you, respectively, enter
                            and exit the level.
                        </p>
                        <div class="alert alert-dark mx-3">
                            <p>
                                Technical notes time! Again. When moving between levels, the following actions occur in this order:
                            </p>
                            <ol>
                                <li>Any continuous actions (ones from the <code>actions</code> array) of the previous level are removed.</li>
                                <li>Any <code>onExit</code> actions of the previous level are triggered.</li>
                                <li>Any <code>onEnter</code> actions of the new level are triggered.</li>
                                <li>Any continuous actions of the new level are applied.</li>
                                <li>The tick ends</li>
                                <li>On the next tick, any needs that were adjusted through <code>adjustNeed</code> are adjusted</li>
                            </ol>
                            <p>
                                Do note that if you extensively use <code>adjustNeed</code> (or its manipulator brethren <code>onNeedChanged</code>), you can
                                ultimately cause loops - as noted above, needs that get adjusted are fired on the next tick, which, if they in turn adjust needs
                                will then adjust them on the tick after that, and so on and so forth. If this ends up adjusting the need which started the process,
                                you <i>could</i> end up adjusting needs every tick, which will cause performance issues.
                            </p>
                        </div>
                        <p class="alert alert-dark mx-3">
                            One more thing, I swear, then we get back to the fun stuff. If you use a <code>adjustNeed</code> action <code>onEnter</code> or <code>onExit</code>,
                            then the need will be adjusted just that once. On the other hand, if you use it as a continuous action, then the need will get adjusted by the amount
                            when you enter the level, and then adjusted by the <i>inverse</i> of the amount when you exit the level; we used this feature above to add
                            5 to the player's attack when they're a Genius, and then take that back (-5) when exiting the level.
                        </p>
                        <p>
                            Phew, okay. Enough technical mumbo jumbo. And enough examples for levels. We're going to leave "Dumb as Rocks" alone, because, isn't just being 
                            called that a good enough penalty for you? Oh, wait. We're not actually calling anybody anything right now, because in all of this, the players
                            can't actually <i>see</i> what's going on. Let's fix that real quick.
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [],
                            "levels": [],
                            "mixins": [
                                {
                                    "type": "ui",
                                    "color": "#99CDC9",
                                    "icon": "w_book01"
                                }
                            ]
                        })}
                        <p>
                            Mixins? What are mixins? Well, they're functionality that you can "mix in" to needs - much like you do with manipulators and level actions - you 
                            can add additional functionality to your needs.
                        </p>
                        <p>
                            Alright. Assuming you've made it here, you have a file that looks like this:
                        </p>
                        {TutorialCode({
                            "name": "Learning",
                            "type": "custom",
                            "min": 0,
                            "max": 100,
                            "initial": 15,
                            "manipulators": [
                                {
                                    "type": "lookingAt",
                                    "amount": 0.05,
                                    "blocks": [
                                        "minecraft:bookshelf"
                                    ]
                                },
                                {
                                    "type": "itemUsed",
                                    "defaultAmount": 5,
                                    "showTooltip": true,
                                    "items": {
                                        "milk_bucket": null,
                                        "rotten_flesh": "-current/2"
                                    },
                                    "formatting": {
                                        "white": "(0,10]"
                                    }
                                },
                                {
                                    "type": "onDeath",
                                    "amount": "min(-current/4, -10)",
                                    "downTo": 15
                                }
                            ],
                            "levels": [
                                {
                                    "name": "Genius",
                                    "min": 80,
                                    "actions": [
                                        {
                                            "type": "potionEffect",
                                            "effect": "jump_boost",
                                            "amplifier": 2
                                        },
                                        {
                                            "type": "adjustNeed",
                                            "need": "attack",
                                            "amount": 5
                                        }
                                    ]
                                },
                                {
                                    "name": "Learned",
                                    "min": 50,
                                    "max": 80,
                                    "actions": [
                                        {
                                            "type": "potionEffect",
                                            "effect": "jump_boost"
                                        }
                                    ]
                                },
                                {
                                    "name": "Dumb as Rocks",
                                    "max": 10
                                }
                            ],
                            "mixins": [
                                {
                                    "type": "ui",
                                    "color": "#99CDC9",
                                    "icon": "w_book01"
                                }
                            ]
                        })}
                        <p>
                            Guess who has two thumbs and wasn't following along with the tutorial and had to go back up to piece all that back together?
                        </p>
                        <p>
                            If you open up your game now, you'll be able to go into your inventory and click on the scroll in the upper-right corner to go to
                            this UI:
                        </p>
                        <div class="text-center">
                            <img src="https://pbs.twimg.com/media/EBYtuCVUcAAL_32?format=png" alt="UI example" class="rounded" />
                        </div>
                        <p>
                            There you go. Your very own custom stat that does things. Now go out and conquer the world. Or at least create new and <del>obtuse</del> interesting
                            stat systems to <del>torture</del> entertain your players with.
                        </p>
                    </div>
                    <div id="needTypes">
                        <h2>Need Types</h2>
                        <div>
                            {this.state.needs}
                        </div>
                    </div>
                    <hr />
                    <div id="mixins">
                        <h2>Mixins</h2>
                        {this.state.mixins}
                    </div>
                    <hr />
                    <div id="manipulators">
                        <h2>Manipulators</h2>
                        {this.state.manipulators}
                    </div>
                    <hr />
                    <div id="levels">
                        <h2>Levels</h2>
                    </div>
                    <hr />
                    <div id="actions">
                        <h2>Actions</h2>
                        {this.state.actions}
                    </div>
                </div>
            </div>
        );
    }
}

ReactDOM.render(<App />, document.getElementById('root'));
