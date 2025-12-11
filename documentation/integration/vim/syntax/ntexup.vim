" ntexup.vim – Syntax highlighting for NTexUp

if exists("b:current_syntax")
  finish
endif

" Comments
syn match ntexupLineComment "//.*$"
syn region ntexupBlockComment start="/\*" end="\*/" contains=ntexupTodo
syn keyword ntexupTodo TODO FIXME XXX BUG

" Strings
syn region ntexupString start=+"+ skip=+\\"+ end=+"+
syn region ntexupString start=+'+ skip=+\\'+ end=+'+

" Numbers
syn match ntexupNumber "\v(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?"

" Symbols
syn match ntexupSymbol "[:(){}\[\];,+\-*/=<>!|&]"

" Keywords (category 1)
syn keyword ntexupKwd1 page page-group import include if else true false
syn keyword ntexupKwd1 styles template ntexup json xml yaml tson
syn keyword ntexupKwd1 arrayLeftRotate arrayRightRotate
syn keyword ntexupKwd1 componentBody either eitherPath
syn keyword ntexupKwd1 rotateSinebowColor lighterColor darkerColor

" Keywords2 (drawing primitives)
syn keyword ntexupKwd2 arc circle ellipse line rectangle polygon polyline points
syn keyword ntexupKwd2 grid group flow gantt
syn keyword ntexupKwd2 sphere cylinder diamond hexagon pentagon octagon nonagon
syn keyword ntexupKwd2 trapezoid triangle triangle-full oval-full parallelogram
syn keyword ntexupKwd2 text txt image img plot2d

" Keywords3 (attributes)
syn keyword ntexupAttr at anchor position top bottom left right center
syn keyword ntexupAttr size width height columns rows
syn keyword ntexupAttr color bg fg stroke fill margin
syn keyword ntexupAttr font-bold font-italic font-size font-underline font-family
syn keyword ntexupAttr debug hide show start-arrow end-arrow

" Keywords4 (document-level metadata)
syn keyword ntexupDoc documentTitle documentSubtitle documentAuthor
syn keyword ntexupDoc documentSection documentSubsection
syn keyword ntexupDoc agenda-slide cover-slide content-slide thankyou-slide

" Highlight Groups Mapping
hi def link ntexupLineComment   Comment
hi def link ntexupBlockComment  Comment
hi def link ntexupTodo          Todo
hi def link ntexupString        String
hi def link ntexupNumber        Number
hi def link ntexupSymbol        Operator

hi def link ntexupKwd1          Keyword
hi def link ntexupKwd2          Type
hi def link ntexupAttr          Identifier
hi def link ntexupDoc           Structure

let b:current_syntax = "ntexup"
