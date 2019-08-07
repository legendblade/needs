import React from 'react';
import ReactDOM from 'react-dom';
import { chain, cloneDeep, clone, startCase } from 'lodash';

const noDesc = (<span class='text-muted'>There's no description of this item; encourage the mod author to add one, either through the API or localization.</span>);

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
        <nav class="navbar sticky-top navbar-light">
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
            <div>
                This can be created through the following type names:
                <ul>
                    {props.aliases.map((a) => (<li key={a}>{a}</li>))}
                </ul>
            </div>
        );
    } else {
        return (<div><small>This cannot be directly added</small></div>);
    }
}

function Field(props) {
    let typeBadge = 'badge float-left mr-1 ' + ((props.isParent) ? 'badge-light' : 'badge-info');
    let expBadge = 'badge float-left ' + ((props.isParent) ? 'badge-secondary' : 'badge-success');
    let nameClass = props.isParent ? 'text-muted' : '';
    let parentText = props.isParent ? [(<br key="0" />), (<span key="1" class='text-muted'><small>From {props.parent}</small></span>)] : [];

    return (
        <tr key={props.name} class='row mx-1'>
            <th class='col-md-4 text-right'>
                <span class={typeBadge}>{props.type}</span>
                {props.isExpression ? (<span class={expBadge}>Expression</span>) : ('')}
                <span class={nameClass}>{props.name}</span>{parentText}
            </th>
            <td class='col-md-8'>{props.description || noDesc}</td>
        </tr>
    );
}

function FieldList(props) {
    if (!props || props.length <= 0) return ('');
    return [
        (<hr key="0" />),
        (<table key="1" class='table table-striped table-borderless'>
            <tbody>
                {props.map((f) => Field(f))}
            </tbody>
        </table>)
    ]
}

function Entry(props, parentFields) {
    props.depth = props.depth || [];
    const name = startCase(props.id);

    const hLvl = (props.depth.length < 4) ? props.depth.length + 3 : 6;
    const Tag = 'h' + hLvl;
    const container =  {
        'marginLeft': (props.depth.length * 20 + 'px')
    };


    const output = [(
        <div class='card mt-4 p-2' style={container}>
            <Tag class='card-title'>{name}<small>{props.depth.map((d) => " < " + d)}</small></Tag>
            <p class='card-subtitle'>{props.description || noDesc}</p>

            <AliasList aliases={props.aliases} />
            <div class='text-muted'>{FieldList(parentFields)}</div>
            {FieldList(props.fields)}
        </div>
    )];

    const pfields = chain(cloneDeep(props.fields))
        .each((f) => {
            f.isParent = true;
            f.parent = name;
        })
        .union(parentFields || [])
        .value();

    const newDepth = chain(clone(props.depth)).union([name]).value();
    props.children.forEach((c) => {
        c.depth = newDepth;
        output.push(Entry(c, pfields))
    });

    return output;
}

class App extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            'navroot': {}
        };
        this.loadAndSetData();
    }

    getData() {
        fetch('./data.json')
            .then((response) => {
                return response.json();
            })
            .then((response) => {                
                const navroot = {
                    "title": "Needs, Wants, and Desires",
                    "root": "#",
                    "class": "navbar-brand",
                    "children": [
                        { "title": "Need Types", "root": "#needs" },
                        { "title": "Mixins", "root": "#mixins" },
                        { "title": "Manipulators", "root": "#manipulators" },
                        { "title": "Levels", "root": "#levels" },
                        { "title": "Actions", "root": "#actions" }
                    ]
                };

                this.setState({
                    'navroot': navroot,
                    'needs': response.needs.map((n) => (Entry(n))),
                    'mixins': response.mixins.map((n) => (Entry(n))),
                    'manipulators': response.manipulators.map((n) => (Entry(n))),
                    'actions': response.actions.map((n) => (Entry(n)))
                });
            });
    }

    loadAndSetData() {
        this.getData();
    }

    render() {
        return (
            <div class="row">
                <div class="col-2">
                    <nav id="scrollnav" class="navbar sticky-top navbar-light">
                        <NavBar value={this.state.navroot} />
                    </nav>
                </div>

                <div class="col-8">
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
                                    to make the player regenerate health when they first enter sunlight - just use the <code>onNeedChanged</code> manipulator to link up the
                                    <code>sunlight</code> need with the <code>health</code> need. But maybe that's too nice, and instead you want to... convince the
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
                                    their associated need by, instead can take a mathematical expression like this:
                                </p>
                                <p class="text-center">
                                    <code class="text-center">current / 2</code>
                                </p>
                                <p>
                                    ... or this:
                                </p>
                                <p class="text-center">
                                    <code>min(current,4) + max(-2,current)</code>
                                </p>
                                <p>
                                    Or anything else you can find over at <a href="http://mathparser.org/mxparser-math-collection/">http://mathparser.org/mxparser-math-collection/</a>
                                    in order to let you have full control over how things work.
                                </p>
                                <p>
                                    Do note that each individual expression may have different variables you can use - be sure to read the description of each to check
                                    what it supports.
                                </p>
                            </dl>
                        </dl>
                        <p>
                            With all that out of the way, let's dive in to the various sections to see what all can be accomplished.
                        </p>
                        <div class="alert alert-light">
                            Note, if you're reading this document fromthe mod's config directory, it's been automatically generated from the mods you have installed - 
                            so if any mod adds an extra need, mixin, manipulator, or level action, it'll show up here after the first time you run the game with that mod installed.
                        </div>
                    </div>
                    <hr />
                    <div id="needs">
                        <h2>Need Types</h2>
                        <div class='container-fluid'>
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
