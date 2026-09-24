interface CountResponse {
  count: number;
}

async function readCount(response: Response, errorMessage: string): Promise<number> {
  if (!response.ok) {
    throw new Error(errorMessage);
  }

  const result = (await response.json()) as CountResponse;
  return result.count;
}

async function updateCount(path: string): Promise<number> {
  const response = await fetch(path, {
    credentials: "include",
    method: "POST",
  });

  return readCount(response, "Unable to update the counter. Please try again.");
}

export async function getCount(): Promise<number> {
  const response = await fetch("/api/count", { credentials: "include" });
  return readCount(response, "Unable to load the counter. Please try again.");
}

export function incrementCount(): Promise<number> {
  return updateCount("/api/count/increment");
}

export function resetCount(): Promise<number> {
  return updateCount("/api/count/reset");
}
