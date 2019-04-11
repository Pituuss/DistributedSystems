defmodule Hospital.Message do
  defstruct message_type: nil, body: nil, name: nil, examination: nil, status: nil, who: nil

  def to_message(%_{} = task) do
    :erlang.term_to_binary(task)
  end

  def from_message(task) when is_binary(task) do
    :erlang.binary_to_term(task)
  end
end
