/* A Bison parser, made by GNU Bison 2.5.  */

/* Bison interface for Yacc-like parsers in C
   
      Copyright (C) 1984, 1989-1990, 2000-2011 Free Software Foundation, Inc.
   
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.
   
   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */


/* Tokens.  */
#ifndef YYTOKENTYPE
# define YYTOKENTYPE
   /* Put the tokens into the symbol table, so that GDB and other debuggers
      know about them.  */
   enum yytokentype {
     IDENT = 258,
     FCONST = 259,
     SCONST = 260,
     BCONST = 261,
     XCONST = 262,
     Op = 263,
     ICONST = 264,
     PARAM = 265,
     TYPECAST = 266,
     DOT_DOT = 267,
     COLON_EQUALS = 268,
     ABORT_P = 269,
     ABSOLUTE_P = 270,
     ACCESS = 271,
     ACTION = 272,
     ADD_P = 273,
     ADMIN = 274,
     AFTER = 275,
     AGGREGATE = 276,
     ALL = 277,
     ALSO = 278,
     ALTER = 279,
     ALWAYS = 280,
     ANALYSE = 281,
     ANALYZE = 282,
     AND = 283,
     ANY = 284,
     ARRAY = 285,
     AS = 286,
     ASC = 287,
     ASSERTION = 288,
     ASSIGNMENT = 289,
     ASYMMETRIC = 290,
     AT = 291,
     ATTRIBUTE = 292,
     AUTHORIZATION = 293,
     BACKWARD = 294,
     BEFORE = 295,
     BEGIN_P = 296,
     BETWEEN = 297,
     BIGINT = 298,
     BINARY = 299,
     BIT = 300,
     BOOLEAN_P = 301,
     BOTH = 302,
     BY = 303,
     CACHE = 304,
     CALLED = 305,
     CASCADE = 306,
     CASCADED = 307,
     CASE = 308,
     CAST = 309,
     CATALOG_P = 310,
     CHAIN = 311,
     CHAR_P = 312,
     CHARACTER = 313,
     CHARACTERISTICS = 314,
     CHECK = 315,
     CHECKPOINT = 316,
     CLASS = 317,
     CLOSE = 318,
     CLUSTER = 319,
     COALESCE = 320,
     COLLATE = 321,
     COLLATION = 322,
     COLUMN = 323,
     COMMENT = 324,
     COMMENTS = 325,
     COMMIT = 326,
     COMMITTED = 327,
     CONCURRENTLY = 328,
     CONFIGURATION = 329,
     CONNECTION = 330,
     CONSTRAINT = 331,
     CONSTRAINTS = 332,
     CONTENT_P = 333,
     CONTINUE_P = 334,
     CONVERSION_P = 335,
     COPY = 336,
     COST = 337,
     CREATE = 338,
     CROSS = 339,
     CSV = 340,
     CURRENT_P = 341,
     CURRENT_CATALOG = 342,
     CURRENT_DATE = 343,
     CURRENT_ROLE = 344,
     CURRENT_SCHEMA = 345,
     CURRENT_TIME = 346,
     CURRENT_TIMESTAMP = 347,
     CURRENT_USER = 348,
     CURSOR = 349,
     CYCLE = 350,
     DATA_P = 351,
     DATABASE = 352,
     DAY_P = 353,
     DEALLOCATE = 354,
     DEC = 355,
     DECIMAL_P = 356,
     DECLARE = 357,
     DEFAULT = 358,
     DEFAULTS = 359,
     DEFERRABLE = 360,
     DEFERRED = 361,
     DEFINER = 362,
     DELETE_P = 363,
     DELIMITER = 364,
     DELIMITERS = 365,
     DESC = 366,
     DICTIONARY = 367,
     DISABLE_P = 368,
     DISCARD = 369,
     DISTINCT = 370,
     DO = 371,
     DOCUMENT_P = 372,
     DOMAIN_P = 373,
     DOUBLE_P = 374,
     DROP = 375,
     EACH = 376,
     ELSE = 377,
     ENABLE_P = 378,
     ENCODING = 379,
     ENCRYPTED = 380,
     END_P = 381,
     ENUM_P = 382,
     ESCAPE = 383,
     EVENT = 384,
     EXCEPT = 385,
     EXCLUDE = 386,
     EXCLUDING = 387,
     EXCLUSIVE = 388,
     EXECUTE = 389,
     EXISTS = 390,
     EXPLAIN = 391,
     EXTENSION = 392,
     EXTERNAL = 393,
     EXTRACT = 394,
     FALSE_P = 395,
     FAMILY = 396,
     FETCH = 397,
     FIRST_P = 398,
     FLOAT_P = 399,
     FOLLOWING = 400,
     FOR = 401,
     FORCE = 402,
     FOREIGN = 403,
     FORWARD = 404,
     FREEZE = 405,
     FROM = 406,
     FULL = 407,
     FUNCTION = 408,
     FUNCTIONS = 409,
     GLOBAL = 410,
     GRANT = 411,
     GRANTED = 412,
     GREATEST = 413,
     GROUP_P = 414,
     HANDLER = 415,
     HAVING = 416,
     HEADER_P = 417,
     HOLD = 418,
     HOUR_P = 419,
     IDENTITY_P = 420,
     IF_P = 421,
     ILIKE = 422,
     IMMEDIATE = 423,
     IMMUTABLE = 424,
     IMPLICIT_P = 425,
     IN_P = 426,
     INCLUDING = 427,
     INCREMENT = 428,
     INDEX = 429,
     INDEXES = 430,
     INHERIT = 431,
     INHERITS = 432,
     INITIALLY = 433,
     INLINE_P = 434,
     INNER_P = 435,
     INOUT = 436,
     INPUT_P = 437,
     INSENSITIVE = 438,
     INSERT = 439,
     INSTEAD = 440,
     INT_P = 441,
     INTEGER = 442,
     INTERSECT = 443,
     INTERVAL = 444,
     INTO = 445,
     INVOKER = 446,
     IS = 447,
     ISNULL = 448,
     ISOLATION = 449,
     JOIN = 450,
     KEY = 451,
     LABEL = 452,
     LANGUAGE = 453,
     LARGE_P = 454,
     LAST_P = 455,
     LATERAL_P = 456,
     LC_COLLATE_P = 457,
     LC_CTYPE_P = 458,
     LEADING = 459,
     LEAKPROOF = 460,
     LEAST = 461,
     LEFT = 462,
     LEVEL = 463,
     LIKE = 464,
     LIMIT = 465,
     LISTEN = 466,
     LOAD = 467,
     LOCAL = 468,
     LOCALTIME = 469,
     LOCALTIMESTAMP = 470,
     LOCATION = 471,
     LOCK_P = 472,
     MAPPING = 473,
     MATCH = 474,
     MATERIALIZED = 475,
     MAXVALUE = 476,
     MINUTE_P = 477,
     MINVALUE = 478,
     MODE = 479,
     MONTH_P = 480,
     MOVE = 481,
     NAME_P = 482,
     NAMES = 483,
     NATIONAL = 484,
     NATURAL = 485,
     NCHAR = 486,
     NEXT = 487,
     NO = 488,
     NONE = 489,
     NOT = 490,
     NOTHING = 491,
     NOTIFY = 492,
     NOTNULL = 493,
     NOWAIT = 494,
     NULL_P = 495,
     NULLIF = 496,
     NULLS_P = 497,
     NUMERIC = 498,
     OBJECT_P = 499,
     OF = 500,
     OFF = 501,
     OFFSET = 502,
     OIDS = 503,
     ON = 504,
     ONLY = 505,
     OPERATOR = 506,
     OPTION = 507,
     OPTIONS = 508,
     OR = 509,
     ORDER = 510,
     OUT_P = 511,
     OUTER_P = 512,
     OVER = 513,
     OVERLAPS = 514,
     OVERLAY = 515,
     OWNED = 516,
     OWNER = 517,
     PARSER = 518,
     PARTIAL = 519,
     PARTITION = 520,
     PASSING = 521,
     PASSWORD = 522,
     PLACING = 523,
     PLANS = 524,
     POSITION = 525,
     PRECEDING = 526,
     PRECISION = 527,
     PRESERVE = 528,
     PREPARE = 529,
     PREPARED = 530,
     PRIMARY = 531,
     PRIOR = 532,
     PRIVILEGES = 533,
     PROCEDURAL = 534,
     PROCEDURE = 535,
     PROGRAM = 536,
     QUOTE = 537,
     RANGE = 538,
     READ = 539,
     REAL = 540,
     REASSIGN = 541,
     RECHECK = 542,
     RECURSIVE = 543,
     REF = 544,
     REFERENCES = 545,
     REFRESH = 546,
     REINDEX = 547,
     RELATIVE_P = 548,
     RELEASE = 549,
     RENAME = 550,
     REPEATABLE = 551,
     REPLACE = 552,
     REPLICA = 553,
     RESET = 554,
     RESTART = 555,
     RESTRICT = 556,
     RETURNING = 557,
     RETURNS = 558,
     REVOKE = 559,
     RIGHT = 560,
     ROLE = 561,
     ROLLBACK = 562,
     ROW = 563,
     ROWS = 564,
     RULE = 565,
     SAVEPOINT = 566,
     SCHEMA = 567,
     SCROLL = 568,
     SEARCH = 569,
     SECOND_P = 570,
     SECURITY = 571,
     SELECT = 572,
     SEQUENCE = 573,
     SEQUENCES = 574,
     SERIALIZABLE = 575,
     SERVER = 576,
     SESSION = 577,
     SESSION_USER = 578,
     SET = 579,
     SETOF = 580,
     SHARE = 581,
     SHOW = 582,
     SIMILAR = 583,
     SIMPLE = 584,
     SMALLINT = 585,
     SNAPSHOT = 586,
     SOME = 587,
     STABLE = 588,
     STANDALONE_P = 589,
     START = 590,
     STATEMENT = 591,
     STATISTICS = 592,
     STDIN = 593,
     STDOUT = 594,
     STORAGE = 595,
     STRICT_P = 596,
     STRIP_P = 597,
     SUBSTRING = 598,
     SYMMETRIC = 599,
     SYSID = 600,
     SYSTEM_P = 601,
     TABLE = 602,
     TABLES = 603,
     TABLESPACE = 604,
     TEMP = 605,
     TEMPLATE = 606,
     TEMPORARY = 607,
     TEXT_P = 608,
     THEN = 609,
     TIME = 610,
     TIMESTAMP = 611,
     TO = 612,
     TRAILING = 613,
     TRANSACTION = 614,
     TREAT = 615,
     TRIGGER = 616,
     TRIM = 617,
     TRUE_P = 618,
     TRUNCATE = 619,
     TRUSTED = 620,
     TYPE_P = 621,
     TYPES_P = 622,
     UNBOUNDED = 623,
     UNCOMMITTED = 624,
     UNENCRYPTED = 625,
     UNION = 626,
     UNIQUE = 627,
     UNKNOWN = 628,
     UNLISTEN = 629,
     UNLOGGED = 630,
     UNTIL = 631,
     UPDATE = 632,
     USER = 633,
     USING = 634,
     VACUUM = 635,
     VALID = 636,
     VALIDATE = 637,
     VALIDATOR = 638,
     VALUE_P = 639,
     VALUES = 640,
     VARCHAR = 641,
     VARIADIC = 642,
     VARYING = 643,
     VERBOSE = 644,
     VERSION_P = 645,
     VIEW = 646,
     VOLATILE = 647,
     WHEN = 648,
     WHERE = 649,
     WHITESPACE_P = 650,
     WINDOW = 651,
     WITH = 652,
     WITHOUT = 653,
     WORK = 654,
     WRAPPER = 655,
     WRITE = 656,
     XML_P = 657,
     XMLATTRIBUTES = 658,
     XMLCONCAT = 659,
     XMLELEMENT = 660,
     XMLEXISTS = 661,
     XMLFOREST = 662,
     XMLPARSE = 663,
     XMLPI = 664,
     XMLROOT = 665,
     XMLSERIALIZE = 666,
     YEAR_P = 667,
     YES_P = 668,
     ZONE = 669,
     NULLS_FIRST = 670,
     NULLS_LAST = 671,
     WITH_TIME = 672,
     POSTFIXOP = 673,
     UMINUS = 674
   };
#endif



#if ! defined YYSTYPE && ! defined YYSTYPE_IS_DECLARED
typedef union YYSTYPE
{

/* Line 2068 of yacc.c  */
#line 178 "gram.y"

	core_YYSTYPE		core_yystype;
	/* these fields must match core_YYSTYPE: */
	int					ival;
	char				*str;
	const char			*keyword;

	char				chr;
	bool				boolean;
	JoinType			jtype;
	DropBehavior		dbehavior;
	OnCommitAction		oncommit;
	List				*list;
	Node				*node;
	Value				*value;
	ObjectType			objtype;
	TypeName			*typnam;
	FunctionParameter   *fun_param;
	FunctionParameterMode fun_param_mode;
	FuncWithArgs		*funwithargs;
	DefElem				*defelt;
	SortBy				*sortby;
	WindowDef			*windef;
	JoinExpr			*jexpr;
	IndexElem			*ielem;
	Alias				*alias;
	RangeVar			*range;
	IntoClause			*into;
	WithClause			*with;
	A_Indices			*aind;
	ResTarget			*target;
	struct PrivTarget	*privtarget;
	AccessPriv			*accesspriv;
	InsertStmt			*istmt;
	VariableSetStmt		*vsetstmt;



/* Line 2068 of yacc.c  */
#line 508 "gram.h"
} YYSTYPE;
# define YYSTYPE_IS_TRIVIAL 1
# define yystype YYSTYPE /* obsolescent; will be withdrawn */
# define YYSTYPE_IS_DECLARED 1
#endif



#if ! defined YYLTYPE && ! defined YYLTYPE_IS_DECLARED
typedef struct YYLTYPE
{
  int first_line;
  int first_column;
  int last_line;
  int last_column;
} YYLTYPE;
# define yyltype YYLTYPE /* obsolescent; will be withdrawn */
# define YYLTYPE_IS_DECLARED 1
# define YYLTYPE_IS_TRIVIAL 1
#endif



