%%
%% Autogenerated by Thrift Compiler (0.12.0)
%%
%% DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
%%

-module(bank_schema_types).

-include("bank_schema_types.hrl").

-export([struct_info/1, struct_info_ext/1, enum_info/1, enum_names/0, struct_names/0, exception_names/0]).

struct_info('Money') ->
  {struct, [{1, double},
          {2, i32}]}
;

struct_info('ID') ->
  {struct, [{1, i64}]}
;

struct_info('Login') ->
  {struct, [{1, {struct, {'bank_schema_types', 'ID'}}},
          {2, {struct, {'bank_schema_types', 'Password'}}}]}
;

struct_info('Password') ->
  {struct, [{1, string}]}
;

struct_info('PersonalData') ->
  {struct, [{1, string},
          {2, string},
          {3, {struct, {'bank_schema_types', 'ID'}}},
          {4, {struct, {'bank_schema_types', 'Money'}}}]}
;

struct_info('Account') ->
  {struct, [{1, {struct, {'bank_schema_types', 'PersonalData'}}},
          {2, {struct, {'bank_schema_types', 'Money'}}},
          {3, {struct, {'bank_schema_types', 'Password'}}},
          {4, i32}]}
;

struct_info('RegisterUserReponse') ->
  {struct, [{1, {struct, {'bank_schema_types', 'Password'}}},
          {2, i32}]}
;

struct_info('ErrorInOperation') ->
  {struct, [{1, string}]}
;

struct_info('LoanRequest') ->
  {struct, [{1, {struct, {'bank_schema_types', 'Login'}}},
          {2, {struct, {'bank_schema_types', 'Money'}}},
          {3, i32}]}
;

struct_info('LoanResponse') ->
  {struct, [{1, bool},
          {2, {struct, {'bank_schema_types', 'Money'}}},
          {3, {struct, {'bank_schema_types', 'Money'}}}]}
;

struct_info(_) -> erlang:error(function_clause).

struct_info_ext('Money') ->
  {struct, [{1, undefined, double, 'amount', undefined},
          {2, undefined, i32, 'currency', undefined}]}
;

struct_info_ext('ID') ->
  {struct, [{1, undefined, i64, 'id', undefined}]}
;

struct_info_ext('Login') ->
  {struct, [{1, undefined, {struct, {'bank_schema_types', 'ID'}}, 'id', #'ID'{}},
          {2, undefined, {struct, {'bank_schema_types', 'Password'}}, 'passwd', undefined}]}
;

struct_info_ext('Password') ->
  {struct, [{1, undefined, string, 'password', undefined}]}
;

struct_info_ext('PersonalData') ->
  {struct, [{1, undefined, string, 'name', undefined},
          {2, undefined, string, 'surname', undefined},
          {3, undefined, {struct, {'bank_schema_types', 'ID'}}, 'id', #'ID'{}},
          {4, undefined, {struct, {'bank_schema_types', 'Money'}}, 'income', #'Money'{}}]}
;

struct_info_ext('Account') ->
  {struct, [{1, undefined, {struct, {'bank_schema_types', 'PersonalData'}}, 'client', #'PersonalData'{}},
          {2, undefined, {struct, {'bank_schema_types', 'Money'}}, 'balance', #'Money'{}},
          {3, undefined, {struct, {'bank_schema_types', 'Password'}}, 'passwd', #'Password'{}},
          {4, undefined, i32, 'accountType', undefined}]}
;

struct_info_ext('RegisterUserReponse') ->
  {struct, [{1, undefined, {struct, {'bank_schema_types', 'Password'}}, 'passwd', #'Password'{}},
          {2, undefined, i32, 'accountType', undefined}]}
;

struct_info_ext('ErrorInOperation') ->
  {struct, [{1, undefined, string, 'reason', undefined}]}
;

struct_info_ext('LoanRequest') ->
  {struct, [{1, undefined, {struct, {'bank_schema_types', 'Login'}}, 'login', #'Login'{}},
          {2, undefined, {struct, {'bank_schema_types', 'Money'}}, 'money', #'Money'{}},
          {3, undefined, i32, 'time', undefined}]}
;

struct_info_ext('LoanResponse') ->
  {struct, [{1, required, bool, 'accepted', undefined},
          {2, optional, {struct, {'bank_schema_types', 'Money'}}, 'nativeBankCurrencyCost', #'Money'{}},
          {3, optional, {struct, {'bank_schema_types', 'Money'}}, 'foreginCurrencyCost', #'Money'{}}]}
;

struct_info_ext(_) -> erlang:error(function_clause).

struct_names() ->
  ['Money', 'ID', 'Login', 'Password', 'PersonalData', 'Account', 'RegisterUserReponse', 'LoanRequest', 'LoanResponse'].

enum_info('Currency') ->
  [
    {'USD', 0},
    {'GBP', 1},
    {'PLN', 2},
    {'EUR', 3},
    {'CHF', 4}
  ];

enum_info('AccountType') ->
  [
    {'Standart', 0},
    {'Premium', 1}
  ];

enum_info(_) -> erlang:error(function_clause).

enum_names() ->
  ['Currency', 'AccountType'].

exception_names() ->
  ['ErrorInOperation'].

