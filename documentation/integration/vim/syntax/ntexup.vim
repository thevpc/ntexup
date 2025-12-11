" ntexup.vim – Syntax highlighting for NTexUp

if exists("b:current_syntax")
  finish
endif

" Comments //////////////////////////////////////////////////////////////
" //  and /* ... */
syn match ntexupLineComment "//.*$"
syn region ntexupBlockComment start="/\*" end="\*/" contains=ntexupTodo
syn keyword ntexupTodo TODO FIXME XXX BUG

" Strings ///////////////////////////////////////////////////////////////
syn region ntexupString start=+"+ skip=+\\."+ end=+"+
syn region ntexupString start=+'+ skip=+\\.+ end=+'+

" Numbers ///////////////////////////////////////////////////////////////
syn match ntexupNumber "\v(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?"

" Symbols //////////////////////////////////////////////////////////////
syn match ntexupSymbol "[:(){}\[\];,+\-*/=<>!|&]"

" Keywords (category 1 in your IntelliJ file) ///////////////////////////
syn keyword ntexupKwd1
      \ page page-group import include if else true false
      \ styles template ntexup json xml yaml tson
      \ arrayLeftRotate arrayRightRotate
      \ componentBody either eitherPath
      \ rotateSinebowColor lighterColor darkerColor

" Keywords2 (drawing primitives) ///////////////////////////////////////
syn keyword ntexupKwd2
      \ arc circle ellipse line rectangle polygon polyline points
      \ grid group flow gantt
      \ sphere cylinder diamond hexagon pentagon octagon nonagon
      \ trapezoid triangle triangle-full oval-full parallelogram
      \ text txt image img plot2d

" Keywords3 (attributes) ///////////////////////////////////////////////
syn keyword ntexupAttr
      \ at anchor position top bottom left right center
      \ size width height columns rows
      \ color bg fg stroke fill margin
      \ font-bold font-italic font-size font-underline font-family
      \ debug hide show start-arrow end-arrow

" Keywords4 (document-level metadata) ///////////////////////////////////
syn keyword ntexupDoc
      \ documentTitle documentSubtitle documentAuthor
      \ documentSection documentSubsection
      \ agenda-slide cover-slide content-slide thankyou-slide

" Highlight Groups Mapping /////////////////////////////////////////////
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
