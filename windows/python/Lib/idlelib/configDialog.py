"""IDLE Configuration Dialog: support user customization of IDLE by GUI

Customize font faces, sizes, and colorization attributes.  Set indentation
defaults.  Customize keybindings.  Colorization and keybindings can be
saved as user defined sets.  Select startup options including shell/editor
and default window size.  Define additional help sources.

Note that tab width in IDLE is currently fixed at eight due to Tk issues.
Refer to comments in EditorWindow autoindent code for details.

"""
from Tkinter import *
import tkMessageBox, tkColorChooser, tkFont

from idlelib.configHandler import idleConf
from idlelib.dynOptionMenuWidget import DynOptionMenu
from idlelib.keybindingDialog import GetKeysDialog
from idlelib.configSectionNameDialog import GetCfgSectionNameDialog
from idlelib.configHelpSourceEdit import GetHelpSourceDialog
from idlelib.tabbedpages import TabbedPageSet
from idlelib import macosxSupport
class ConfigDialog(Toplevel):

    def __init__(self, parent, title='', _htest=False, _utest=False):
        """
        _htest - bool, change box location when running htest
        _utest - bool, don't wait_window when running unittest
        """
        Toplevel.__init__(self, parent)
        self.parent = parent
        if _htest:
            parent.instance_dict = {}
        self.wm_withdraw()

        self.configure(borderwidth=5)
        self.title(title or 'IDLE Preferences')
        self.geometry(
                "+%d+%d" % (parent.winfo_rootx() + 20,
                parent.winfo_rooty() + (30 if not _htest else 150)))
        #Theme Elements. Each theme element key is its display name.
        #The first value of the tuple is the sample area tag name.
        #The second value is the display name list sort index.
        self.themeElements={
            'Normal Text':('normal', '00'),
            'Python Keywords':('keyword', '01'),
            'Python Definitions':('definition', '02'),
            'Python Builtins':('builtin', '03'),
            'Python Comments':('comment', '04'),
            'Python Strings':('string', '05'),
            'Selected Text':('hilite', '06'),
            'Found Text':('hit', '07'),
            'Cursor':('cursor', '08'),
            'Error Text':('error', '09'),
            'Shell Normal Text':('console', '10'),
            'Shell Stdout Text':('stdout', '11'),
            'Shell Stderr Text':('stderr', '12'),
            }
        self.ResetChangedItems() #load initial values in changed items dict
        self.CreateWidgets()
        self.resizable(height=FALSE, width=FALSE)
        self.transient(parent)
        self.grab_set()
        self.protocol("WM_DELETE_WINDOW", self.Cancel)
        self.tabPages.focus_set()
        #key bindings for this dialog
        #self.bind('<Escape>', self.Cancel) #dismiss dialog, no save
        #self.bind('<Alt-a>', self.Apply) #apply changes, save
        #self.bind('<F1>', self.Help) #context help
        self.LoadConfigs()
        self.AttachVarCallbacks() #avoid callbacks during LoadConfigs

        if not _utest:
            self.wm_deiconify()
            self.wait_window()

    def CreateWidgets(self):
        self.tabPages = TabbedPageSet(self,
                page_names=['Fonts/Tabs', 'Highlighting', 'Keys', 'General'])
        self.tabPages.pack(side=TOP, expand=TRUE, fill=BOTH)
        self.CreatePageFontTab()
        self.CreatePageHighlight()
        self.CreatePageKeys()
        self.CreatePageGeneral()
        self.create_action_buttons().pack(side=BOTTOM)
    def create_action_buttons(self):
        if macosxSupport.isAquaTk():
            # Changing the default padding on OSX results in unreadable
            # text in the buttons
            paddingArgs = {}
        else:
            paddingArgs = {'padx':6, 'pady':3}
        outer = Frame(self, pady=2)
        buttons = Frame(outer, pady=2)
        self.buttonOk = Button(
                buttons, text='Ok', command=self.Ok,
                takefocus=FALSE, **paddingArgs)
        self.buttonApply = Button(
                buttons, text='Apply', command=self.Apply,
                takefocus=FALSE, **paddingArgs)
        self.buttonCancel = Button(
                buttons, text='Cancel', command=self.Cancel,
                takefocus=FALSE, **paddingArgs)
        self.buttonOk.pack(side=LEFT, padx=5)
        self.buttonApply.pack(side=LEFT, padx=5)
        self.buttonCancel.pack(side=LEFT, padx=5)
# Comment out Help button creation and packing until implement self.Help
##        self.buttonHelp = Button(
##                buttons, text='Help', command=self.Help,
##                takefocus=FALSE, **paddingArgs)
##        self.buttonHelp.pack(side=RIGHT, padx=5)

        # add space above buttons
        Frame(outer, height=2, borderwidth=0).pack(side=TOP)
        buttons.pack(side=BOTTOM)
        return outer
    def CreatePageFontTab(self):
        parent = self.parent
        self.fontSize = StringVar(parent)
        self.fontBold = BooleanVar(parent)
        self.fontName = StringVar(parent)
        self.spaceNum = IntVar(parent)
        self.editFont = tkFont.Font(parent, ('courier', 10, 'normal'))

        ##widget creation
        #body frame
        frame = self.tabPages.pages['Fonts/Tabs'].frame
        #body section frames
        frameFont = LabelFrame(
                frame, borderwidth=2, relief=GROOVE, text=' Base Editor Font ')
        frameIndent = LabelFrame(
                frame, borderwidth=2, relief=GROOVE, text=' Indentation Width ')
        #frameFont
        frameFontName = Frame(frameFont)
        frameFontParam = Frame(frameFont)
        labelFontNameTitle = Label(
                frameFontName, justify=LEFT, text='Font Face :')
        self.listFontName = Listbox(
                frameFontName, height=5, takefocus=FALSE, exportselection=FALSE)
        self.listFontName.bind(
                '<ButtonRelease-1>', self.OnListFontButtonRelease)
        scrollFont = Scrollbar(frameFontName)
        scrollFont.config(command=self.listFontName.yview)
        self.listFontName.config(yscrollcommand=scrollFont.set)
        labelFontSizeTitle = Label(frameFontParam, text='Size :')
        self.optMenuFontSize = DynOptionMenu(
                frameFontParam, self.fontSize, None, command=self.SetFontSample)
        checkFontBold = Checkbutton(
                frameFontParam, variable=self.fontBold, onvalue=1,
                offvalue=0, text='Bold', command=self.SetFontSample)
        frameFontSample = Frame(frameFont, relief=SOLID, borderwidth=1)
        self.labelFontSample = Label(
                frameFontSample, justify=LEFT, font=self.editFont,
                text='AaBbCcDdEe\nFfGgHhIiJjK\n1234567890\n#:+=(){}[]')
        #frameIndent
        frameIndentSize = Frame(frameIndent)
        labelSpaceNumTitle = Label(
                frameIndentSize, justify=LEFT,
                text='Python Standard: 4 Spaces!')
        self.scaleSpaceNum = Scale(
                frameIndentSize, variable=self.spaceNum,
                orient='horizontal', tickinterval=2, from_=2, to=16)

        #widget packing
        #body
        frameFont.pack(side=LEFT, padx=5, pady=5, expand=TRUE, fill=BOTH)
        frameIndent.pack(side=LEFT, padx=5, pady=5, fill=Y)
        #frameFont
        frameFontName.pack(side=TOP, padx=5, pady=5, fill=X)
        frameFontParam.pack(side=TOP, padx=5, pady=5, fill=X)
        labelFontNameTitle.pack(side=TOP, anchor=W)
        self.listFontName.pack(side=LEFT, expand=TRUE, fill=X)
        scrollFont.pack(side=LEFT, fill=Y)
        labelFontSizeTitle.pack(side=LEFT, anchor=W)
        self.optMenuFontSize.pack(side=LEFT, anchor=W)
        checkFontBold.pack(side=LEFT, anchor=W, padx=20)
        frameFontSample.pack(side=TOP, padx=5, pady=5, expand=TRUE, fill=BOTH)
        self.labelFontSample.pack(expand=TRUE, fill=BOTH)
        #frameIndent
        frameIndentSize.pack(side=TOP, fill=X)
        labelSpaceNumTitle.pack(side=TOP, anchor=W, padx=5)
        self.scaleSpaceNum.pack(side=TOP, padx=5, fill=X)
        return frame

    def CreatePageHighlight(self):
        parent = self.parent
        self.builtinTheme = StringVar(parent)
        self.customTheme = StringVar(parent)
        self.fgHilite = BooleanVar(parent)
        self.colour = StringVar(parent)
        self.fontName = StringVar(parent)
        self.themeIsBuiltin = BooleanVar(parent)
        self.highlightTarget = StringVar(parent)

        ##widget creation
        #body frame
        frame = self.tabPages.pages['Highlighting'].frame
        #body section frames
        frameCustom = LabelFrame(frame, borderwidth=2, relief=GROOVE,
                                 text=' Custom Highlighting ')
        frameTheme = LabelFrame(frame, borderwidth=2, relief=GROOVE,
                                text=' Highlighting Theme ')
        #frameCustom
        self.textHighlightSample=Text(
                frameCustom, relief=SOLID, borderwidth=1,
                font=('courier', 12, ''), cursor='hand2', width=21, height=11,
                takefocus=FALSE, highlightthickness=0, wrap=NONE)
        text=self.textHighlightSample
        text.bind('<Double-Button-1>', lambda e: 'break')
        text.bind('<B1-Motion>', lambda e: 'break')
        textAndTags=(
            ('#you can click here', 'comment'), ('\n', 'normal'),
            ('#to choose items', 'comment'), ('\n', 'normal'),
            ('def', 'keyword'), (' ', 'normal'),
            ('func', 'definition'), ('(param):\n  ', 'normal'),
            ('"""string"""', 'string'), ('\n  var0 = ', 'normal'),
            ("'string'", 'string'), ('\n  var1 = ', 'normal'),
            ("'selected'", 'hilite'), ('\n  var2 = ', 'normal'),
            ("'found'", 'hit'), ('\n  var3 = ', 'normal'),
            ('list', 'builtin'), ('(', 'normal'),
            ('None', 'builtin'), (')\n\n', 'normal'),
            (' error ', 'error'), (' ', 'normal'),
            ('cursor |', 'cursor'), ('\n ', 'normal'),
            ('shell', 'console'), (' ', 'normal'),
            ('stdout', 'stdout'), (' ', 'normal'),
            ('stderr', 'stderr'), ('\n', 'normal'))
        for txTa in textAndTags:
            text.insert(END, txTa[0], txTa[1])
        for element in self.themeElements:
            def tem(event, elem=element):
                event.widget.winfo_toplevel().highlightTarget.set(elem)
            text.tag_bind(
                    self.themeElements[element][0], '<ButtonPress-1>', tem)
        text.config(state=DISABLED)
        self.frameColourSet = Frame(frameCustom, relief=SOLID, borderwidth=1)
        frameFgBg = Frame(frameCustom)
        buttonSetColour = Button(
                self.frameColourSet, text='Choose Colour for :',
                command=self.GetColour, highlightthickness=0)
        self.optMenuHighlightTarget = DynOptionMenu(
                self.frameColourSet, self.highlightTarget, None,
                highlightthickness=0) #, command=self.SetHighlightTargetBinding
        self.radioFg = Radiobutton(
                frameFgBg, variable=self.fgHilite, value=1,
                text='Foreground', command=self.SetColourSampleBinding)
        self.radioBg=Radiobutton(
                frameFgBg, variable=self.fgHilite, value=0,
                text='Background', command=self.SetColourSampleBinding)
        self.fgHilite.set(1)
        buttonSaveCustomTheme = Button(
                frameCustom, text='Save as New Custom Theme',
                command=self.SaveAsNewTheme)
        #frameTheme
        labelTypeTitle = Label(frameTheme, text='Select : ')
        self.radioThemeBuiltin = Radiobutton(
                frameTheme, variable=self.themeIsBuiltin, value=1,
                command=self.SetThemeType, text='a Built-in Theme')
        self.radioThemeCustom = Radiobutton(
                frameTheme, variable=self.themeIsBuiltin, value=0,
                command=self.SetThemeType, text='a Custom Theme')
        self.optMenuThemeBuiltin = DynOptionMenu(
                frameTheme, self.builtinTheme, None, command=None)
        self.optMenuThemeCustom=DynOptionMenu(
                frameTheme, self.customTheme, None, command=None)
        self.buttonDeleteCustomTheme=Button(
                frameTheme, text='Delete Custom Theme',
                command=self.DeleteCustomTheme)

        ##widget packing
        #body
        frameCustom.pack(side=LEFT, padx=5, pady=5, expand=TRUE, fill=BOTH)
        frameTheme.pack(side=LEFT, padx=5, pady=5, fill=Y)
        #frameCustom
        self.frameColourSet.pack(side=TOP, padx=5, pady=5, expand=TRUE, fill=X)
        frameFgBg.pack(side=TOP, padx=5, pady=0)
        self.textHighlightSample.pack(
                side=TOP, padx=5, pady=5, expand=TRUE, fill=BOTH)
        buttonSetColour.pack(side=TOP, expand=TRUE, fill=X, padx=8, pady=4)
        self.optMenuHighlightTarget.pack(
                side=TOP, expand=TRUE, fill=X, padx=8, pady=3)
        self.radioFg.pack(side=LEFT, anchor=E)
        self.radioBg.pack(side=RIGHT, anchor=W)
        buttonSaveCustomTheme.pack(side=BOTTOM, fill=X, padx=5, pady=5)
        #frameTheme
        labelTypeTitle.pack(side=TOP, anchor=W, padx=5, pady=5)
        self.radioThemeBuiltin.pack(side=TOP, anchor=W, padx=5)
        self.radioThemeCustom.pack(side=TOP, anchor=W, padx=5, pady=2)
        self.optMenuThemeBuiltin.pack(side=TOP, fill=X, padx=5, pady=5)
        self.optMenuThemeCustom.pack(side=TOP, fill=X, anchor=W, padx=5, pady=5)
        self.buttonDeleteCustomTheme.pack(side=TOP, fill=X, padx=5, pady=5)
        return frame

    def CreatePageKeys(self):
        parent = self.parent
        self.bindingTarget = StringVar(parent)
        self.builtinKeys = StringVar(parent)
        self.customKeys = StringVar(parent)
        self.keysAreBuiltin = BooleanVar(parent)
        self.keyBinding = StringVar(parent)

        ##widget creation
        #body frame
        frame = self.tabPages.pages['Keys'].frame
        #body section frames
        frameCustom = LabelFrame(
                frame, borderwidth=2, relief=GROOVE,
                text=' Custom Key Bindings ')
        frameKeySets = LabelFrame(
                frame, borderwidth=2, relief=GROOVE, text=' Key Set ')
        #frameCustom
        frameTarget = Frame(frameCustom)
        labelTargetTitle = Label(frameTarget, text='Action - Key(s)')
        scrollTargetY = Scrollbar(frameTarget)
        scrollTargetX = Scrollbar(frameTarget, orient=HORIZONTAL)
        self.listBindings = Listbox(
                frameTarget, takefocus=FALSE, exportselection=FALSE)
        self.listBindings.bind('<ButtonRelease-1>', self.KeyBindingSelected)
        scrollTargetY.config(command=self.listBindings.yview)
        scrollTargetX.config(command=self.listBindings.xview)
        self.listBindings.config(yscrollcommand=scrollTargetY.set)
        self.listBindings.config(xscrollcommand=scrollTargetX.set)
        self.buttonNewKeys = Button(
                frameCustom, text='Get New Keys for Selection',
                command=self.GetNewKeys, state=DISABLED)
        #frameKeySets
        frames = [Frame(frameKeySets, padx=2, pady=2, borderwidth=0)
                  for i in range(2)]
        self.radioKeysBuiltin = Radiobutton(
                frames[0], variable=self.keysAreBuiltin, value=1,
                command=self.SetKeysType, text='Use a Built-in Key Set')
        self.radioKeysCustom = Radiobutton(
                frames[0], variable=self.keysAreBuiltin,  value=0,
                command=self.SetKeysType, text='Use a Custom Key Set')
        self.optMenuKeysBuiltin = DynOptionMenu(
                frames[0], self.builtinKeys, None, command=None)
        self.optMenuKeysCustom = DynOptionMenu(
                frames[0], self.customKeys, None, command=None)
        self.buttonDeleteCustomKeys = Button(
                frames[1], text='Delete Custom Key Set',
                command=self.DeleteCustomKeys)
        buttonSaveCustomKeys = Button(
                frames[1], text='Save as New Custom Key Set',
                command=self.SaveAsNewKeySet)

        ##widget packing
        #body
        frameCustom.pack(side=BOTTOM, padx=5, pady=5, expand=TRUE, fill=BOTH)
        frameKeySets.pack(side=BOTTOM, padx=5, pady=5, fill=BOTH)
        #frameCustom
        self.buttonNewKeys.pack(side=BOTTOM, fill=X, padx=5, pady=5)
        frameTarget.pack(side=LEFT, padx=5, pady=5, expand=TRUE, fill=BOTH)
        #frame target
        frameTarget.columnconfigure(0, weight=1)
        frameTarget.rowconfigure(1, weight=1)
        labelTargetTitle.grid(row=0, column=0, columnspan=2, sticky=W)
        self.listBindings.grid(row=1, column=0, sticky=NSEW)
        scrollTargetY.grid(row=1, column=1, sticky=NS)
        scrollTargetX.grid(row=2, column=0, sticky=EW)
        #frameKeySets
        self.radioKeysBuiltin.grid(row=0, column=0, sticky=W+NS)
        self.radioKeysCustom.grid(row=1, column=0, sticky=W+NS)
        self.optMenuKeysBuiltin.grid(row=0, column=1, sticky=NSEW)
        self.optMenuKeysCustom.grid(row=1, column=1, sticky=NSEW)
        self.buttonDeleteCustomKeys.pack(side=LEFT, fill=X, expand=True, padx=2)
        buttonSaveCustomKeys.pack(side=LEFT, fill=X, expand=True, padx=2)
        frames[0].pack(side=TOP, fill=BOTH, expand=True)
        frames[1].pack(side=TOP, fill=X, expand=True, pady=2)
        return frame

    def CreatePageGeneral(self):
        parent = self.parent
        self.winWidth = StringVar(parent)
        self.winHeight = StringVar(parent)
        self.startupEdit = IntVar(parent)
        self.autoSave = IntVar(parent)
        self.encoding = StringVar(parent)
        self.userHelpBrowser = BooleanVar(parent)
        self.helpBrowser = StringVar(parent)

        #widget creation
        #body
        frame = self.tabPages.pages['General'].frame
        #body section frames
        frameRun = LabelFrame(frame, borderwidth=2, relief=GROOVE,
                              text=' Startup Preferences ')
        frameSave = LabelFrame(frame, borderwidth=2, relief=GROOVE,
                               text=' Autosave Preferences ')
        frameWinSize = Frame(frame, borderwidth=2, relief=GROOVE)
        frameEncoding = Frame(frame, borderwidth=2, relief=GROOVE)
        frameHelp = LabelFrame(frame, borderwidth=2, relief=GROOVE,
                               text=' Additional Help Sources ')
        #frameRun
        labelRunChoiceTitle = Label(frameRun, text='At Startup')
        radioStartupEdit = Radiobutton(
                frameRun, variable=self.startupEdit, value=1,
                command=self.SetKeysType, text="Open Edit Window")
        radioStartupShell = Radiobutton(
                frameRun, variable=self.startupEdit, value=0,
                command=self.SetKeysType, text='Open Shell Window')
        #frameSave
        labelRunSaveTitle = Label(frameSave, text='At Start of Run (F5)  ')
        radioSaveAsk = Radiobutton(
                frameSave, variable=self.autoSave, value=0,
                command=self.SetKeysType, text="Prompt to Save")
        radioSaveAuto = Radiobutton(
                frameSave, variable=self.autoSave, value=1,
                command=self.SetKeysType, text='No Prompt')
        #frameWinSize
        labelWinSizeTitle = Label(
                frameWinSize, text='Initial Window Size  (in characters)')
        labelWinWidthTitle = Label(frameWinSize, text='Width')
        entryWinWidth = Entry(
                frameWinSize, textvariable=self.winWidth, width=3)
        labelWinHeightTitle = Label(frameWinSize, text='Height')
        entryWinHeight = Entry(
                frameWinSize, textvariable=self.winHeight, width=3)
        #frameEncoding
        labelEncodingTitle = Label(
                frameEncoding, text="Default Source Encoding")
        radioEncLocale = Radiobutton(
                frameEncoding, variable=self.encoding,
                value="locale", text="Locale-defined")
        radioEncUTF8 = Radiobutton(
                frameEncoding, variable=self.encoding,
                value="utf-8", text="UTF-8")
        radioEncNone = Radiobutton(
                frameEncoding, variable=self.encoding,
                value="none", text="None")
        #frameHelp
        frameHelpList = Frame(frameHelp)
        frameHelpListButtons = Frame(frameHelpList)
        scrollHelpList = Scrollbar(frameHelpList)
        self.listHelp = Listbox(
                frameHelpList, height=5, takefocus=FALSE,
                exportselection=FALSE)
        scrollHelpList.config(command=self.listHelp.yview)
        self.listHelp.config(yscrollcommand=scrollHelpList.set)
        self.listHelp.bind('<ButtonRelease-1>', self.HelpSourceSelected)
        self.buttonHelpListEdit = Button(
                frameHelpListButtons, text='Edit', state=DISABLED,
                width=8, command=self.HelpListItemEdit)
        self.buttonHelpListAdd = Button(
                frameHelpListButtons, text='Add',
                width=8, command=self.HelpListItemAdd)
        self.buttonHelpListRemove = Button(
                frameHelpListButtons, text='Remove', state=DISABLED,
                width=8, command=self.HelpListItemRemove)

        #widget packing
        #body
        frameRun.pack(side=TOP, padx=5, pady=5, fill=X)
        frameSave.pack(side=TOP, padx=5, pady=5, fill=X)
        frameWinSize.pack(side=TOP, padx=5, pady=5, fill=X)
        frameEncoding.pack(side=TOP, padx=5, pady=5, fill=X)
        frameHelp.pack(side=TOP, padx=5, pady=5, expand=TRUE, fill=BOTH)
        #frameRun
        labelRunChoiceTitle.pack(side=LEFT, anchor=W, padx=5, pady=5)
        radioStartupShell.pack(side=RIGHT, anchor=W, padx=5, pady=5)
        radioStartupEdit.pack(side=RIGHT, anchor=W, padx=5, pady=5)
        #frameSave
        labelRunSaveTitle.pack(side=LEFT, anchor=W, padx=5, pady=5)
        radioSaveAuto.pack(side=RIGHT, anchor=W, padx=5, pady=5)
        radioSaveAsk.pack(side=RIGHT, anchor=W, padx=5, pady=5)
        #frameWinSize
        labelWinSizeTitle.pack(side=LEFT, anchor=W, padx=5, pady=5)
        entryWinHeight.pack(side=RIGHT, anchor=E, padx=10, pady=5)
        labelWinHeightTitle.pack(side=RIGHT, anchor=E, pady=5)
        entryWinWidth.pack(side=RIGHT, anchor=E, padx=10, pady=5)
        labelWinWidthTitle.pack(side=RIGHT, anchor=E, pady=5)
        #frameEncoding
        labelEncodingTitle.pack(side=LEFT, anchor=W, padx=5, pady=5)
        radioEncNone.pack(side=RIGHT, anchor=E, pady=5)
        radioEncUTF8.pack(side=RIGHT, anchor=E, pady=5)
        radioEncLocale.pack(side=RIGHT, anchor=E, pady=5)
        #frameHelp
        frameHelpListButtons.pack(side=RIGHT, padx=5, pady=5, fill=Y)
        frameHelpList.pack(side=TOP, padx=5, pady=5, expand=TRUE, fill=BOTH)
        scrollHelpList.pack(side=RIGHT, anchor=W, fill=Y)
        self.listHelp.pack(side=LEFT, anchor=E, expand=TRUE, fill=BOTH)
        self.buttonHelpListEdit.pack(side=TOP, anchor=W, pady=5)
        self.buttonHelpListAdd.pack(side=TOP, anchor=W)
        self.buttonHelpListRemove.pack(side=TOP, anchor=W, pady=5)
        return frame

    def AttachVarCallbacks(self):
        self.fontSize.trace_variable('w', self.VarChanged_fontSize)
        self.fontName.trace_variable('w', self.VarChanged_fontName)
        self.fontBold.trace_variable('w', self.VarChanged_fontBold)
        self.spaceNum.trace_variable('w', self.VarChanged_spaceNum)
        self.colour.trace_variable('w', self.VarChanged_colour)
        self.builtinTheme.trace_variable('w', self.VarChanged_builtinTheme)
        self.customTheme.trace_variable('w', self.VarChanged_customTheme)
        self.themeIsBuiltin.trace_variable('w', self.VarChanged_themeIsBuiltin)
        self.highlightTarget.trace_variable('w', self.VarChanged_highlightTarget)
        self.keyBinding.trace_variable('w', self.VarChanged_keyBinding)
        self.builtinKeys.trace_variable('w', self.VarChanged_builtinKeys)
        self.customKeys.trace_variable('w', self.VarChanged_customKeys)
        self.keysAreBuiltin.trace_variable('w', self.VarChanged_keysAreBuiltin)
        self.winWidth.trace_variable('w', self.VarChanged_winWidth)
        self.winHeight.trace_variable('w', self.VarChanged_winHeight)
        self.startupEdit.trace_variable('w', self.VarChanged_startupEdit)
        self.autoSave.trace_variable('w', self.VarChanged_autoSave)
        self.encoding.trace_variable('w', self.VarChanged_encoding)

    def VarChanged_fontSize(self, *params):
        value = self.fontSize.get()
        self.AddChangedItem('main', 'EditorWindow', 'font-size', value)

    def VarChanged_fontName(self, *params):
        value = self.fontName.get()
        self.AddChangedItem('main', 'EditorWindow', 'font', value)

    def VarChanged_fontBold(self, *params):
        value = self.fontBold.get()
        self.AddChangedItem('main', 'EditorWindow', 'font-bold', value)

    def VarChanged_spaceNum(self, *params):
        value = self.spaceNum.get()
        self.AddChangedItem('main', 'Indent', 'num-spaces', value)

    def VarChanged_colour(self, *params):
        self.OnNewColourSet()

    def VarChanged_builtinTheme(self, *params):
        value = self.builtinTheme.get()
        self.AddChangedItem('main', 'Theme', 'name', value)
        self.PaintThemeSample()

    def VarChanged_customTheme(self, *params):
        value = self.customTheme.get()
        if value != '- no custom themes -':
            self.AddChangedItem('main', 'Theme', 'name', value)
            self.PaintThemeSample()

    def VarChanged_themeIsBuiltin(self, *params):
        value = self.themeIsBuiltin.get()
        self.AddChangedItem('main', 'Theme', 'default', value)
        if value:
            self.VarChanged_builtinTheme()
        else:
            self.VarChanged_customTheme()

    def VarChanged_highlightTarget(self, *params):
        self.SetHighlightTarget()

    def VarChanged_keyBinding(self, *params):
        value = self.keyBinding.get()
        keySet = self.customKeys.get()
        event = self.listBindings.get(ANCHOR).split()[0]
        if idleConf.IsCoreBinding(event):
            #this is a core keybinding
            self.AddChangedItem('keys', keySet, event, value)
        else: #this is an extension key binding
            extName = idleConf.GetExtnNameForEvent(event)
            extKeybindSection = extName + '_cfgBindings'
            self.AddChangedItem('extensions', extKeybindSection, event, value)

    def VarChanged_builtinKeys(self, *params):
        value = self.builtinKeys.get()
        self.AddChangedItem('main', 'Keys', 'name', value)
        self.LoadKeysList(value)

    def VarChanged_customKeys(self, *params):
        value = self.customKeys.get()
        if value != '- no custom keys -':
            self.AddChangedItem('main', 'Keys', 'name', value)
            self.LoadKeysList(value)

    def VarChanged_keysAreBuiltin(self, *params):
        value = self.keysAreBuiltin.get()
        self.AddChangedItem('main', 'Keys', 'default', value)
        if value:
            self.VarChanged_builtinKeys()
        else:
            self.VarChanged_customKeys()

    def VarChanged_winWidth(self, *params):
        value = self.winWidth.get()
        self.AddChangedItem('main', 'EditorWindow', 'width', value)

    def VarChanged_winHeight(self, *params):
        value = self.winHeight.get()
        self.AddChangedItem('main', 'EditorWindow', 'height', value)

    def VarChanged_startupEdit(self, *params):
        value = self.startupEdit.get()
        self.AddChangedItem('main', 'General', 'editor-on-startup', value)

    def VarChanged_autoSave(self, *params):
        value = self.autoSave.get()
        self.AddChangedItem('main', 'General', 'autosave', value)

    def VarChanged_encoding(self, *params):
        value = self.encoding.get()
        self.AddChangedItem('main', 'EditorWindow', 'encoding', value)

    def ResetChangedItems(self):
        #When any config item is changed in this dialog, an entry
        #should be made in the relevant section (config type) of this
        #dictionary. The key should be the config file section name and the
        #value a dictionary, whose key:value pairs are item=value pairs for
        #that config file section.
        self.changedItems = {'main':{}, 'highlight':{}, 'keys':{},
                             'extensions':{}}

    def AddChangedItem(self, typ, section, item, value):
        value = str(value) #make sure we use a string
        if section not in self.changedItems[typ]:
            self.changedItems[typ][section] = {}
        self.changedItems[typ][section][item] = value

    def GetDefaultItems(self):
        dItems={'main':{}, 'highlight':{}, 'keys':{}, 'extensions':{}}
        for configType in dItems:
            sections = idleConf.GetSectionList('default', configType)
            for section in sections:
                dItems[configType][section] = {}
                options = idleConf.defaultCfg[configType].GetOptionList(section)
                for option in options:
                    dItems[configType][section][option] = (
                            idleConf.defaultCfg[configType].Get(section, option))
        return dItems

    def SetThemeType(self):
        if self.themeIsBuiltin.get():
            self.optMenuThemeBuiltin.config(state=NORMAL)
            self.optMenuThemeCustom.config(state=DISABLED)
            self.buttonDeleteCustomTheme.config(state=DISABLED)
        else:
            self.optMenuThemeBuiltin.config(state=DISABLED)
            self.radioThemeCustom.config(state=NORMAL)
            self.optMenuThemeCustom.config(state=NORMAL)
            self.buttonDeleteCustomTheme.config(state=NORMAL)

    def SetKeysType(self):
        if self.keysAreBuiltin.get():
            self.optMenuKeysBuiltin.config(state=NORMAL)
            self.optMenuKeysCustom.config(state=DISABLED)
            self.buttonDeleteCustomKeys.config(state=DISABLED)
        else:
            self.optMenuKeysBuiltin.config(state=DISABLED)
            self.radioKeysCustom.config(state=NORMAL)
            self.optMenuKeysCustom.config(state=NORMAL)
            self.buttonDeleteCustomKeys.config(state=NORMAL)

    def GetNewKeys(self):
        listIndex = self.listBindings.index(ANCHOR)
        binding = self.listBindings.get(listIndex)
        bindName = binding.split()[0] #first part, up to first space
        if self.keysAreBuiltin.get():
            currentKeySetName = self.builtinKeys.get()
        else:
            currentKeySetName = self.customKeys.get()
        currentBindings = idleConf.GetCurrentKeySet()
        if currentKeySetName in self.changedItems['keys']: #unsaved changes
            keySetChanges = self.changedItems['keys'][currentKeySetName]
            for event in keySetChanges:
                currentBindings[event] = keySetChanges[event].split()
        currentKeySequences = currentBindings.values()
        newKeys = GetKeysDialog(self, 'Get New Keys', bindName,
                currentKeySequences).result
        if newKeys: #new keys were specified
            if self.keysAreBuiltin.get(): #current key set is a built-in
                message = ('Your changes will be saved as a new Custom Key Set.'
                           ' Enter a name for your new Custom Key Set below.')
                newKeySet = self.GetNewKeysName(message)
                if not newKeySet: #user cancelled custom key set creation
                    self.listBindings.select_set(listIndex)
                    self.listBindings.select_anchor(listIndex)
                    return
                else: #create new custom key set based on previously active key set
                    self.CreateNewKeySet(newKeySet)
            self.listBindings.delete(listIndex)
            self.listBindings.insert(listIndex, bindName+' - '+newKeys)
            self.listBindings.select_set(listIndex)
            self.listBindings.select_anchor(listIndex)
            self.keyBinding.set(newKeys)
        else:
            self.listBindings.select_set(listIndex)
            self.listBindings.select_anchor(listIndex)

    def GetNewKeysName(self, message):
        usedNames = (idleConf.GetSectionList('user', 'keys') +
                idleConf.GetSectionList('default', 'keys'))
        newKeySet = GetCfgSectionNameDialog(
                self, 'New Custom Key Set', message, usedNames).result
        return newKeySet

    def SaveAsNewKeySet(self):
        newKeysName = self.GetNewKeysName('New Key Set Name:')
        if newKeysName:
            self.CreateNewKeySet(newKeysName)

    def KeyBindingSelected(self, event):
        self.buttonNewKeys.config(state=NORMAL)

    def CreateNewKeySet(self, newKeySetName):
        #creates new custom key set based on the previously active key set,
        #and makes the new key set active
        if self.keysAreBuiltin.get():
            prevKeySetName = self.builtinKeys.get()
        else:
            prevKeySetName = self.customKeys.get()
        prevKeys = idleConf.GetCoreKeys(prevKeySetName)
        newKeys = {}
        for event in prevKeys: #add key set to changed items
            eventName = event[2:-2] #trim off the angle brackets
            binding = ' '.join(prevKeys[event])
            newKeys[eventName] = binding
        #handle any unsaved changes to prev key set
        if prevKeySetName in self.changedItems['keys']:
            keySetChanges = self.changedItems['keys'][prevKeySetName]
            for event in keySetChanges:
                newKeys[event] = keySetChanges[event]
        #save the new theme
        self.SaveNewKeySet(newKeySetName, newKeys)
        #change gui over to the new key set
        customKeyList = idleConf.GetSectionList('user', 'keys')
        customKeyList.sort()
        self.optMenuKeysCustom.SetMenu(customKeyList, newKeySetName)
        self.keysAreBuiltin.set(0)
        self.SetKeysType()

    def LoadKeysList(self, keySetName):
        reselect = 0
        newKeySet = 0
        if self.listBindings.curselection():
            reselect = 1
            listIndex = self.listBindings.index(ANCHOR)
        keySet = idleConf.GetKeySet(keySetName)
        bindNames = keySet.keys()
        bindNames.sort()
        self.listBindings.delete(0, END)
        for bindName in bindNames:
            key = ' '.join(keySet[bindName]) #make key(s) into a string
            bindName = bindName[2:-2] #trim off the angle brackets
            if keySetName in self.changedItems['keys']:
                #handle any unsaved changes to this key set
                if bindName in self.changedItems['keys'][keySetName]:
                    key = self.changedItems['keys'][keySetName][bindName]
            self.listBindings.insert(END, bindName+' - '+key)
        if reselect:
            self.listBindings.see(listIndex)
            self.listBindings.select_set(listIndex)
            self.listBindings.select_anchor(listIndex)

    def DeleteCustomKeys(self):
        keySetName=self.customKeys.get()
        delmsg = 'Are you sure you wish to delete the key set %r ?'
        if not tkMessageBox.askyesno(
                'Delete Key Set',  delmsg % keySetName, parent=self):
            return
        #remove key set from config
        idleConf.userCfg['keys'].remove_section(keySetName)
        if keySetName in self.changedItems['keys']:
            del(self.changedItems['keys'][keySetName])
        #write changes
        idleConf.userCfg['keys'].Save()
        #reload user key set list
        itemList = idleConf.GetSectionList('user', 'keys')
        itemList.sort()
        if not itemList:
            self.radioKeysCustom.config(state=DISABLED)
            self.optMenuKeysCustom.SetMenu(itemList, '- no custom keys -')
        else:
            self.optMenuKeysCustom.SetMenu(itemList, itemList[0])
        #revert to default key set
        self.keysAreBuiltin.set(idleConf.defaultCfg['main'].Get('Keys', 'default'))
        self.builtinKeys.set(idleConf.defaultCfg['main'].Get('Keys', 'name'))
        #user can't back out of these changes, they must be applied now
        self.Apply()
        self.SetKeysType()

    def DeleteCustomTheme(self):
        themeName = self.customTheme.get()
        delmsg = 'Are you sure you wish to delete the theme %r ?'
        if not tkMessageBox.askyesno(
                'Delete Theme',  delmsg % themeName, parent=self):
            return
        #remove theme from config
        idleConf.userCfg['highlight'].remove_section(themeName)
        if themeName in self.changedItems['highlight']:
            del(self.changedItems['highlight'][themeName])
        #write changes
        idleConf.userCfg['highlight'].Save()
        #reload user theme list
        itemList = idleConf.GetSectionList('user', 'highlight')
        itemList.sort()
        if not itemList:
            self.radioThemeCustom.config(state=DISABLED)
            self.optMenuThemeCustom.SetMenu(itemList, '- no custom themes -')
        else:
            self.optMenuThemeCustom.SetMenu(itemList, itemList[0])
        #revert to default theme
        self.themeIsBuiltin.set(idleConf.defaultCfg['main'].Get('Theme', 'default'))
        self.builtinTheme.set(idleConf.defaultCfg['main'].Get('Theme', 'name'))
        #user can't back out of these changes, they must be applied now
        self.Apply()
        self.SetThemeType()

    def GetColour(self):
        target = self.highlightTarget.get()
        prevColour = self.frameColourSet.cget('bg')
        rgbTuplet, colourString = tkColorChooser.askcolor(
                parent=self, title='Pick new colour for : '+target,
                initialcolor=prevColour)
        if colourString and (colourString != prevColour):
            #user didn't cancel, and they chose a new colour
            if self.themeIsBuiltin.get():  #current theme is a built-in
                message = ('Your changes will be saved as a new Custom Theme. '
                           'Enter a name for your new Custom Theme below.')
                newTheme = self.GetNewThemeName(message)
                if not newTheme:  #user cancelled custom theme creation
                    return
                else:  #create new custom theme based on previously active theme
                    self.CreateNewTheme(newTheme)
                    self.colour.set(colourString)
            else:  #current theme is user defined
                self.colour.set(colourString)

    def OnNewColourSet(self):
        newColour=self.colour.get()
        self.frameColourSet.config(bg=newColour)  #set sample
        plane ='foreground' if self.fgHilite.get() else 'background'
        sampleElement = self.themeElements[self.highlightTarget.get()][0]
        self.textHighlightSample.tag_config(sampleElement, **{plane:newColour})
        theme = self.customTheme.get()
        themeElement = sampleElement + '-' + plane
        self.AddChangedItem('highlight', theme, themeElement, newColour)

    def GetNewThemeName(self, message):
        usedNames = (idleConf.GetSectionList('user', 'highlight') +
                idleConf.GetSectionList('default', 'highlight'))
        newTheme = GetCfgSectionNameDialog(
                self, 'New Custom Theme', message, usedNames).result
        return newTheme

    def SaveAsNewTheme(self):
        newThemeName = self.GetNewThemeName('New Theme Name:')
        if newThemeName:
            self.CreateNewTheme(newThemeName)

    def CreateNewTheme(self, newThemeName):
        #creates new custom theme based on the previously active theme,
        #and makes the new theme active
        if self.themeIsBuiltin.get():
            themeType = 'default'
            themeName = self.builtinTheme.get()
        else:
            themeType = 'user'
            themeName = self.customTheme.get()
        newTheme = idleConf.GetThemeDict(themeType, themeName)
        #apply any of the old theme's unsaved changes to the new theme
        if themeName in self.changedItems['highlight']:
            themeChanges = self.changedItems['highlight'][themeName]
            for element in themeChanges:
                newTheme[element] = themeChanges[element]
        #save the new theme
        self.SaveNewTheme(newThemeName, newTheme)
        #change gui over to the new theme
        customThemeList = idleConf.GetSectionList('user', 'highlight')
        customThemeList.sort()
        self.optMenuThemeCustom.SetMenu(customThemeList, newThemeName)
        self.themeIsBuiltin.set(0)
        self.SetThemeType()

    def OnListFontButtonRelease(self, event):
        font = self.listFontName.get(ANCHOR)
        self.fontName.set(font.lower())
        self.SetFontSample()

    def SetFontSample(self, event=None):
        fontName = self.fontName.get()
        fontWeight = tkFont.BOLD if self.fontBold.get() else tkFont.NORMAL
        newFont = (fontName, self.fontSize.get(), fontWeight)
        self.labelFontSample.config(font=newFont)
        self.textHighlightSample.configure(font=newFont)

    def SetHighlightTarget(self):
        if self.highlightTarget.get() == 'Cursor':  #bg not possible
            self.radioFg.config(state=DISABLED)
            self.radioBg.config(state=DISABLED)
            self.fgHilite.set(1)
        else:  #both fg and bg can be set
            self.radioFg.config(state=NORMAL)
            self.radioBg.config(state=NORMAL)
            self.fgHilite.set(1)
        self.SetColourSample()

    def SetColourSampleBinding(self, *args):
        self.SetColourSample()

    def SetColourSample(self):
        #set the colour smaple area
        tag = self.themeElements[self.highlightTarget.get()][0]
        plane = 'foreground' if self.fgHilite.get() else 'background'
        colour = self.textHighlightSample.tag_cget(tag, plane)
        self.frameColourSet.config(bg=colour)

    def PaintThemeSample(self):
        if self.themeIsBuiltin.get():  #a default theme
            theme = self.builtinTheme.get()
        else:  #a user theme
            theme = self.customTheme.get()
        for elementTitle in self.themeElements:
            element = self.themeElements[elementTitle][0]
            colours = idleConf.GetHighlight(theme, element)
            if element == 'cursor': #cursor sample needs special painting
                colours['background'] = idleConf.GetHighlight(
                        theme, 'normal', fgBg='bg')
            #handle any unsaved changes to this theme
            if theme in self.changedItems['highlight']:
                themeDict = self.changedItems['highlight'][theme]
                if element + '-foreground' in themeDict:
                    colours['foreground'] = themeDict[element + '-foreground']
                if element + '-background' in themeDict:
                    colours['background'] = themeDict[element + '-background']
            self.textHighlightSample.tag_config(element, **colours)
        self.SetColourSample()

    def HelpSourceSelected(self, event):
        self.SetHelpListButtonStates()

    def SetHelpListButtonStates(self):
        if self.listHelp.size() < 1:  #no entries in list
            self.buttonHelpListEdit.config(state=DISABLED)
            self.buttonHelpListRemove.config(state=DISABLED)
        else: #there are some entries
            if self.listHelp.curselection():  #there currently is a selection
                self.buttonHelpListEdit.config(state=NORMAL)
                self.buttonHelpListRemove.config(state=NORMAL)
            else:  #there currently is not a selection
                self.buttonHelpListEdit.config(state=DISABLED)
                self.buttonHelpListRemove.config(state=DISABLED)

    def HelpListItemAdd(self):
        helpSource = GetHelpSourceDialog(self, 'New Help Source').result
        if helpSource:
            self.userHelpList.append((helpSource[0], helpSource[1]))
            self.listHelp.insert(END, helpSource[0])
            self.UpdateUserHelpChangedItems()
        self.SetHelpListButtonStates()

    def HelpListItemEdit(self):
        itemIndex = self.listHelp.index(ANCHOR)
        helpSource = self.userHelpList[itemIndex]
        newHelpSource = GetHelpSourceDialog(
                self, 'Edit Help Source', menuItem=helpSource[0],
                filePath=helpSource[1]).result
        if (not newHelpSource) or (newHelpSource == helpSource):
            return #no changes
        self.userHelpList[itemIndex] = newHelpSource
        self.listHelp.delete(itemIndex)
        self.listHelp.insert(itemIndex, newHelpSource[0])
        self.UpdateUserHelpChangedItems()
        self.SetHelpListButtonStates()

    def HelpListItemRemove(self):
        itemIndex = self.listHelp.index(ANCHOR)
        del(self.userHelpList[itemIndex])
        self.listHelp.delete(itemIndex)
        self.UpdateUserHelpChangedItems()
        self.SetHelpListButtonStates()

    def UpdateUserHelpChangedItems(self):
        "Clear and rebuild the HelpFiles section in self.changedItems"
        self.changedItems['main']['HelpFiles'] = {}
        for num in range(1, len(self.userHelpList) + 1):
            self.AddChangedItem(
                    'main', 'HelpFiles', str(num),
                    ';'.join(self.userHelpList[num-1][:2]))

    def LoadFontCfg(self):
        ##base editor font selection list
        fonts = list(tkFont.families(self))
        fonts.sort()
        for font in fonts:
            self.listFontName.insert(END, font)
        configuredFont = idleConf.GetOption(
                'main', 'EditorWindow', 'font', default='courier')
        lc_configuredFont = configuredFont.lower()
        self.fontName.set(lc_configuredFont)
        lc_fonts = [s.lower() for s in fonts]
        if lc_configuredFont in lc_fonts:
            currentFontIndex = lc_fonts.index(lc_configuredFont)
            self.listFontName.see(currentFontIndex)
            self.listFontName.select_set(currentFontIndex)
            self.listFontName.select_anchor(currentFontIndex)
        ##font size dropdown
        fontSize = idleConf.GetOption(
                'main', 'EditorWindow', 'font-size', type='int', default='10')
        self.optMenuFontSize.SetMenu(('7', '8', '9', '10', '11', '12', '13',
                                      '14', '16', '18', '20', '22'), fontSize )
        ##fontWeight
        self.fontBold.set(idleConf.GetOption(
                'main', 'EditorWindow', 'font-bold', default=0, type='bool'))
        ##font sample
        self.SetFontSample()

    def LoadTabCfg(self):
        ##indent sizes
        spaceNum = idleConf.GetOption(
            'main', 'Indent', 'num-spaces', default=4, type='int')
        self.spaceNum.set(spaceNum)

    def LoadThemeCfg(self):
        ##current theme type radiobutton
        self.themeIsBuiltin.set(idleConf.GetOption(
                'main', 'Theme', 'default', type='bool', default=1))
        ##currently set theme
        currentOption = idleConf.CurrentTheme()
        ##load available theme option menus
        if self.themeIsBuiltin.get(): #default theme selected
            itemList = idleConf.GetSectionList('default', 'highlight')
            itemList.sort()
            self.optMenuThemeBuiltin.SetMenu(itemList, currentOption)
            itemList = idleConf.GetSectionList('user', 'highlight')
            itemList.sort()
            if not itemList:
                self.radioThemeCustom.config(state=DISABLED)
                self.customTheme.set('- no custom themes -')
            else:
                self.optMenuThemeCustom.SetMenu(itemList, itemList[0])
        else: #user theme selected
            itemList = idleConf.GetSectionList('user', 'highlight')
            itemList.sort()
            self.optMenuThemeCustom.SetMenu(itemList, currentOption)
            itemList = idleConf.GetSectionList('default', 'highlight')
            itemList.sort()
            self.optMenuThemeBuiltin.SetMenu(itemList, itemList[0])
        self.SetThemeType()
        ##load theme element option menu
        themeNames = self.themeElements.keys()
        themeNames.sort(key=lambda x: self.themeElements[x][1])
        self.optMenuHighlightTarget.SetMenu(themeNames, themeNames[0])
        self.PaintThemeSample()
        self.SetHighlightTarget()

    def LoadKeyCfg(self):
        ##current keys type radiobutton
        self.keysAreBuiltin.set(idleConf.GetOption(
                'main', 'Keys', 'default', type='bool', default=1))
        ##currently set keys
        currentOption = idleConf.CurrentKeys()
        ##load available keyset option menus
        if self.keysAreBuiltin.get(): #default theme selected
            itemList = idleConf.GetSectionList('default', 'keys')
            itemList.sort()
            self.optMenuKeysBuiltin.SetMenu(itemList, currentOption)
            itemList = idleConf.GetSectionList('user', 'keys')
            itemList.sort()
            if not itemList:
                self.radioKeysCustom.config(state=DISABLED)
                self.customKeys.set('- no custom keys -')
            else:
                self.optMenuKeysCustom.SetMenu(itemList, itemList[0])
        else: #user key set selected
            itemList = idleConf.GetSectionList('user', 'keys')
            itemList.sort()
            self.optMenuKeysCustom.SetMenu(itemList, currentOption)
            itemList = idleConf.GetSectionList('default', 'keys')
            itemList.sort()
            self.optMenuKeysBuiltin.SetMenu(itemList, itemList[0])
        self.SetKeysType()
        ##load keyset element list
        keySetName = idleConf.CurrentKeys()
        self.LoadKeysList(keySetName)

    def LoadGeneralCfg(self):
        #startup state
        self.startupEdit.set(idleConf.GetOption(
                'main', 'General', 'editor-on-startup', default=1, type='bool'))
        #autosave state
        self.autoSave.set(idleConf.GetOption(
                'main', 'General', 'autosave', default=0, type='bool'))
        #initial window size
        self.winWidth.set(idleConf.GetOption(
                'main', 'EditorWindow', 'width', type='int'))
        self.winHeight.set(idleConf.GetOption(
                'main', 'EditorWindow', 'height', type='int'))
        # default source encoding
        self.encoding.set(idleConf.GetOption(
                'main', 'EditorWindow', 'encoding', default='none'))
        # additional help sources
        self.userHelpList = idleConf.GetAllExtraHelpSourcesList()
        for helpItem in self.userHelpList:
            self.listHelp.insert(END, helpItem[0])
        self.SetHelpListButtonStates()

    def LoadConfigs(self):
        """
        load configuration from default and user config files and populate
        the widgets on the config dialog pages.
        """
        ### fonts / tabs page
        self.LoadFontCfg()
        self.LoadTabCfg()
        ### highlighting page
        self.LoadThemeCfg()
        ### keys page
        self.LoadKeyCfg()
        ### general page
        self.LoadGeneralCfg()

    def SaveNewKeySet(self, keySetName, keySet):
        """
        save a newly created core key set.
        keySetName - string, the name of the new key set
        keySet - dictionary containing the new key set
        """
        if not idleConf.userCfg['keys'].has_section(keySetName):
            idleConf.userCfg['keys'].add_section(keySetName)
        for event in keySet:
            value = keySet[event]
            idleConf.userCfg['keys'].SetOption(keySetName, event, value)

    def SaveNewTheme(self, themeName, theme):
        """
        save a newly created theme.
        themeName - string, the name of the new theme
        theme - dictionary containing the new theme
        """
        if not idleConf.userCfg['highlight'].has_section(themeName):
            idleConf.userCfg['highlight'].add_section(themeName)
        for element in theme:
            value = theme[element]
            idleConf.userCfg['highlight'].SetOption(themeName, element, value)

    def SetUserValue(self, configType, section, item, value):
        if idleConf.defaultCfg[configType].has_option(section, item):
            if idleConf.defaultCfg[configType].Get(section, item) == value:
                #the setting equals a default setting, remove it from user cfg
                return idleConf.userCfg[configType].RemoveOption(section, item)
        #if we got here set the option
        return idleConf.userCfg[configType].SetOption(section, item, value)

    def SaveAllChangedConfigs(self):
        "Save configuration changes to the user config file."
        idleConf.userCfg['main'].Save()
        for configType in self.changedItems:
            cfgTypeHasChanges = False
            for section in self.changedItems[configType]:
                if section == 'HelpFiles':
                    #this section gets completely replaced
                    idleConf.userCfg['main'].remove_section('HelpFiles')
                    cfgTypeHasChanges = True
                for item in self.changedItems[configType][section]:
                    value = self.changedItems[configType][section][item]
                    if self.SetUserValue(configType, section, item, value):
                        cfgTypeHasChanges = True
            if cfgTypeHasChanges:
                idleConf.userCfg[configType].Save()
        for configType in ['keys', 'highlight']:
            # save these even if unchanged!
            idleConf.userCfg[configType].Save()
        self.ResetChangedItems() #clear the changed items dict

    def DeactivateCurrentConfig(self):
        #Before a config is saved, some cleanup of current
        #config must be done - remove the previous keybindings
        winInstances = self.parent.instance_dict
        for instance in winInstances:
            instance.RemoveKeybindings()

    def ActivateConfigChanges(self):
        "Dynamically apply configuration changes"
        winInstances = self.parent.instance_dict.keys()
        for instance in winInstances:
            instance.ResetColorizer()
            instance.ResetFont()
            instance.set_notabs_indentwidth()
            instance.ApplyKeybindings()
            instance.reset_help_menu_entries()

    def Cancel(self):
        self.destroy()

    def Ok(self):
        self.Apply()
        self.destroy()

    def Apply(self):
        self.DeactivateCurrentConfig()
        self.SaveAllChangedConfigs()
        self.ActivateConfigChanges()

    def Help(self):
        pass

class VerticalScrolledFrame(Frame):
    """A pure Tkinter vertically scrollable frame.

    * Use the 'interior' attribute to place widgets inside the scrollable frame
    * Construct and pack/place/grid normally
    * This frame only allows vertical scrolling
    """
    def __init__(self, parent, *args, **kw):
        Frame.__init__(self, parent, *args, **kw)

        # create a canvas object and a vertical scrollbar for scrolling it
        vscrollbar = Scrollbar(self, orient=VERTICAL)
        vscrollbar.pack(fill=Y, side=RIGHT, expand=FALSE)
        canvas = Canvas(self, bd=0, highlightthickness=0,
                        yscrollcommand=vscrollbar.set)
        canvas.pack(side=LEFT, fill=BOTH, expand=TRUE)
        vscrollbar.config(command=canvas.yview)

        # reset the view
        canvas.xview_moveto(0)
        canvas.yview_moveto(0)

        # create a frame inside the canvas which will be scrolled with it
        self.interior = interior = Frame(canvas)
        interior_id = canvas.create_window(0, 0, window=interior, anchor=NW)

        # track changes to the canvas and frame width and sync them,
        # also updating the scrollbar
        def _configure_interior(event):
            # update the scrollbars to match the size of the inner frame
            size = (interior.winfo_reqwidth(), interior.winfo_reqheight())
            canvas.config(scrollregion="0 0 %s %s" % size)
            if interior.winfo_reqwidth() != canvas.winfo_width():
                # update the canvas's width to fit the inner frame
                canvas.config(width=interior.winfo_reqwidth())
        interior.bind('<Configure>', _configure_interior)

        def _configure_canvas(event):
            if interior.winfo_reqwidth() != canvas.winfo_width():
                # update the inner frame's width to fill the canvas
                canvas.itemconfigure(interior_id, width=canvas.winfo_width())
        canvas.bind('<Configure>', _configure_canvas)

        return

def is_int(s):
    "Return 's is blank or represents an int'"
    if not s:
        return True
    try:
        int(s)
        return True
    except ValueError:
        return False

# TODO:
# * Revert to default(s)? Per option or per extension?
# * List options in their original order (possible??)
class ConfigExtensionsDialog(Toplevel):
    """A dialog for configuring IDLE extensions.

    This dialog is generic - it works for any and all IDLE extensions.

    IDLE extensions save their configuration options using idleConf.
    ConfigExtensionsDialog reads the current configuration using idleConf,
    supplies a GUI interface to change the configuration values, and saves the
    changes using idleConf.

    Not all changes take effect immediately - some may require restarting IDLE.
    This depends on each extension's implementation.

    All values are treated as text, and it is up to the user to supply
    reasonable values. The only exception to this are the 'enable*' options,
    which are boolean, and can be toggled with an True/False button.
    """
    def __init__(self, parent, title=None, _htest=False):
        Toplevel.__init__(self, parent)
        self.wm_withdraw()

        self.configure(borderwidth=5)
        self.geometry(
                "+%d+%d" % (parent.winfo_rootx() + 20,
                parent.winfo_rooty() + (30 if not _htest else 150)))
        self.wm_title(title or 'IDLE Extensions Configuration')

        self.defaultCfg = idleConf.defaultCfg['extensions']
        self.userCfg = idleConf.userCfg['extensions']
        self.is_int = self.register(is_int)
        self.load_extensions()
        self.create_widgets()

        self.resizable(height=FALSE, width=FALSE) # don't allow resizing yet
        self.transient(parent)
        self.protocol("WM_DELETE_WINDOW", self.Cancel)
        self.tabbed_page_set.focus_set()
        # wait for window to be generated
        self.update()
        # set current width as the minimum width
        self.wm_minsize(self.winfo_width(), 1)
        # now allow resizing
        self.resizable(height=TRUE, width=TRUE)

        self.wm_deiconify()
        if not _htest:
            self.grab_set()
            self.wait_window()

    def load_extensions(self):
        "Fill self.extensions with data from the default and user configs."
        self.extensions = {}
        for ext_name in idleConf.GetExtensions(active_only=False):
            self.extensions[ext_name] = []

        for ext_name in self.extensions:
            opt_list = sorted(self.defaultCfg.GetOptionList(ext_name))

            # bring 'enable' options to the beginning of the list
            enables = [opt_name for opt_name in opt_list
                       if opt_name.startswith('enable')]
            for opt_name in enables:
                opt_list.remove(opt_name)
            opt_list = enables + opt_list

            for opt_name in opt_list:
                def_str = self.defaultCfg.Get(
                        ext_name, opt_name, raw=True)
                try:
                    def_obj = {'True':True, 'False':False}[def_str]
                    opt_type = 'bool'
                except KeyError:
                    try:
                        def_obj = int(def_str)
                        opt_type = 'int'
                    except ValueError:
                        def_obj = def_str
                        opt_type = None
                try:
                    value = self.userCfg.Get(
                            ext_name, opt_name, type=opt_type, raw=True,
                            default=def_obj)
                except ValueError:  # Need this until .Get fixed
                    value = def_obj  # bad values overwritten by entry
                var = StringVar(self)
                var.set(str(value))

                self.extensions[ext_name].append({'name': opt_name,
                                                  'type': opt_type,
                                                  'default': def_str,
                                                  'value': value,
                                                  'var': var,
                                                 })

    def create_widgets(self):
        """Create the dialog's widgets."""
        self.rowconfigure(0, weight=1)
        self.rowconfigure(1, weight=0)
        self.columnconfigure(0, weight=1)

        # create the tabbed pages
        self.tabbed_page_set = TabbedPageSet(
                self, page_names=self.extensions.keys(),
                n_rows=None, max_tabs_per_row=5,
                page_class=TabbedPageSet.PageRemove)
        self.tabbed_page_set.grid(row=0, column=0, sticky=NSEW)
        for ext_name in self.extensions:
            self.create_tab_page(ext_name)

        self.create_action_buttons().grid(row=1)

    create_action_buttons = ConfigDialog.create_action_buttons.im_func

    def create_tab_page(self, ext_name):
        """Create the page for an extension."""

        page = LabelFrame(self.tabbed_page_set.pages[ext_name].frame,
                          border=2, padx=2, relief=GROOVE,
                          text=' %s ' % ext_name)
        page.pack(fill=BOTH, expand=True, padx=12, pady=2)

        # create the scrollable frame which will contain the entries
        scrolled_frame = VerticalScrolledFrame(page, pady=2, height=250)
        scrolled_frame.pack(side=BOTTOM, fill=BOTH, expand=TRUE)
        entry_area = scrolled_frame.interior
        entry_area.columnconfigure(0, weight=0)
        entry_area.columnconfigure(1, weight=1)

        # create an entry for each configuration option
        for row, opt in enumerate(self.extensions[ext_name]):
            # create a row with a label and entry/checkbutton
            label = Label(entry_area, text=opt['name'])
            label.grid(row=row, column=0, sticky=NW)
            var = opt['var']
            if opt['type'] == 'bool':
                Checkbutton(entry_area, textvariable=var, variable=var,
                            onvalue='True', offvalue='False',
                            indicatoron=FALSE, selectcolor='', width=8
                    ).grid(row=row, column=1, sticky=W, padx=7)
            elif opt['type'] == 'int':
                Entry(entry_area, textvariable=var, validate='key',
                    validatecommand=(self.is_int, '%P')
                    ).grid(row=row, column=1, sticky=NSEW, padx=7)

            else:
                Entry(entry_area, textvariable=var
                    ).grid(row=row, column=1, sticky=NSEW, padx=7)
        return


    Ok = ConfigDialog.Ok.im_func

    def Apply(self):
        self.save_all_changed_configs()
        pass

    Cancel = ConfigDialog.Cancel.im_func

    def Help(self):
        pass

    def set_user_value(self, section, opt):
        name = opt['name']
        default = opt['default']
        value = opt['var'].get().strip() or default
        opt['var'].set(value)
        # if self.defaultCfg.has_section(section):
        # Currently, always true; if not, indent to return
        if (value == default):
            return self.userCfg.RemoveOption(section, name)
        # set the option
        return self.userCfg.SetOption(section, name, value)

    def save_all_changed_configs(self):
        """Save configuration changes to the user config file."""
        has_changes = False
        for ext_name in self.extensions:
            options = self.extensions[ext_name]
            for opt in options:
                if self.set_user_value(ext_name, opt):
                    has_changes = True
        if has_changes:
            self.userCfg.Save()


if __name__ == '__main__':
    import unittest
    unittest.main('idlelib.idle_test.test_configdialog',
                  verbosity=2, exit=False)
    from idlelib.idle_test.htest import run
    run(ConfigDialog, ConfigExtensionsDialog)
